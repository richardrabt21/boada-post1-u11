# boada-post1-u11 — Refactorización Guiada por Pruebas

**Curso:** Patrones de Diseño de Software · Unidad 11  
**Estudiante:** Richard Boada  
**Repositorio:** https://github.com/richardrabt21/boada-post1-u11

> Post-contenido — Refactorización Guiada por Pruebas: Code Smells, Paquetes y Acoplamiento en un Proyecto Java Real

---

## Estructura del proyecto

```
refactoring-u11/
├── pom.xml
└── src/
    ├── main/java/com/universidad/refactoringu11/
    │   ├── RefactoringU11Application.java
    │   ├── inventario/
    │   │   ├── TipoProducto.java          ← enum con polimorfismo (Parte 1)
    │   │   ├── Producto.java
    │   │   ├── InventarioService.java     ← Long Method eliminado (Parte 1)
    │   │   ├── ClasificadorProducto.java  ← Switch eliminado (Parte 1)
    │   │   ├── EtiquetadoProducto.java    ← Switch eliminado (Parte 1)
    │   │   └── NotificadorBajoStock.java  ← interfaz DIP (Parte 2)
    │   └── reportes/
    │       └── ReporteBajoStockService.java ← implementa NotificadorBajoStock
    └── test/java/com/universidad/refactoringu11/
        ├── inventario/
        │   ├── InventarioServiceTest.java
        │   ├── ClasificadorProductoTest.java
        │   └── EtiquetadoProductoTest.java
        └── ArquitecturaPaquetesTest.java
```

---

## Parte 1 — Red de Seguridad y Refactorización Guiada por Evidencia

### Code Smell 1: Long Method en `actualizarInventario()`

#### Antes de refactorizar

El método `actualizarInventario()` tenía **52 líneas** y mezclaba **5 responsabilidades** en un único bloque:

| # | Responsabilidad | Líneas aprox. |
|---|---|---|
| 1 | Validación de entrada | 4 |
| 2 | Búsqueda del producto | 5 |
| 3 | Actualización de stock | 8 |
| 4 | Cálculo de cantidad de reorden | 3 |
| 5 | Registro/alerta | 10 |

```java
// ANTES — un solo método de >35 líneas mezclando 5 responsabilidades
public String actualizarInventario(Long productoId, int cantidad, String tipoMovimiento) {
    // Validacion de entrada
    if (productoId == null || cantidad == 0
            || (!tipoMovimiento.equals("ENTRADA") && !tipoMovimiento.equals("SALIDA"))) {
        return "ERROR_DATOS_INVALIDOS";
    }
    // Busqueda del producto (mezclada con la logica de negocio)
    Producto producto = null;
    for (Producto p : productos) {
        if (p.getId().equals(productoId)) { producto = p; break; }
    }
    if (producto == null) return "ERROR_PRODUCTO_NO_EXISTE";
    // Actualizacion de stock
    int nuevoStock;
    if (tipoMovimiento.equals("ENTRADA")) {
        nuevoStock = producto.getStock() + cantidad;
    } else {
        nuevoStock = producto.getStock() - cantidad;
        if (nuevoStock < 0) return "ERROR_STOCK_INSUFICIENTE";
    }
    producto.setStock(nuevoStock);
    // Calculo de reorden
    boolean necesitaReorden = nuevoStock < producto.getStockMinimo();
    int cantidadReorden = necesitaReorden ? (producto.getStockMinimo() * 3) - nuevoStock : 0;
    // Registro
    String registro = tipoMovimiento + " de " + cantidad + " unidades de "
            + producto.getNombre() + " -> stock final: " + nuevoStock;
    movimientos.add(registro);
    // Alerta
    if (necesitaReorden) {
        System.out.println("ALERTA: ...");
        return "OK_BAJO_STOCK_REORDEN_" + cantidadReorden;
    }
    return "OK_" + nuevoStock;
}
```

#### Después de refactorizar (técnica: *Extract Method* — Fowler, cap. 6)

```java
// DESPUÉS — método principal de 10 líneas, delega en métodos privados nombrados
public String actualizarInventario(Long productoId, int cantidad, String tipoMovimiento) {
    if (!esEntradaValida(productoId, cantidad, tipoMovimiento)) return "ERROR_DATOS_INVALIDOS";
    Producto producto = buscarProducto(productoId);
    if (producto == null) return "ERROR_PRODUCTO_NO_EXISTE";
    String errorStock = aplicarMovimiento(producto, cantidad, tipoMovimiento);
    if (errorStock != null) return errorStock;
    int nuevoStock = producto.getStock();
    registrarMovimiento(producto, cantidad, tipoMovimiento, nuevoStock);
    return evaluarReorden(producto, nuevoStock);
}
```

**Resultado:** `actualizarInventario()` pasó de **52 → 10 líneas**. Cada responsabilidad vive en su propio método privado con nombre que revela intención.

---

### Code Smell 2: Switch Statements duplicados sobre `TipoProducto`

#### Antes de refactorizar

El mismo `switch` sobre `TipoProducto` con las 4 ramas idénticas aparecía en dos clases distintas:

```java
// ClasificadorProducto.java — switch #1
public double calcularRecargoEnvio(Producto producto) {
    switch (producto.getTipo()) {
        case LIBRO:       return 0.0;
        case ELECTRONICO: return producto.getPrecio() * 0.08;
        case ROPA:        return producto.getPrecio() * 0.03;
        case ALIMENTO:    return 2.50;
        default: throw new IllegalArgumentException("...");
    }
}

// EtiquetadoProducto.java — switch #2 (mismo esqueleto, distinta lógica)
public String generarEtiqueta(Producto producto) {
    switch (producto.getTipo()) {
        case LIBRO:       return "[LIBRO] " + ...;
        case ELECTRONICO: return "[ELECTRO] " + ...;
        case ROPA:        return "[ROPA] " + ...;
        case ALIMENTO:    return "[ALIMENTO] " + ...;
        default: throw new IllegalArgumentException("...");
    }
}
```

**Problema:** agregar un tipo nuevo (p. ej. `DEPORTE`) obligaba a modificar las **dos** clases.

#### Después de refactorizar (técnica: *Replace Conditional with Polymorphism* — Fowler, cap. 10)

```java
// TipoProducto.java — cada constante implementa su propio comportamiento
public enum TipoProducto {
    LIBRO {
        public double calcularRecargoEnvio(double precio) { return 0.0; }
        public String generarEtiqueta(String nombre) { return "[LIBRO] " + nombre + " - Exento de IVA"; }
    },
    ELECTRONICO {
        public double calcularRecargoEnvio(double precio) { return precio * 0.08; }
        public String generarEtiqueta(String nombre) { return "[ELECTRO] " + nombre + " - Garantia 1 ano"; }
    },
    // ... ROPA, ALIMENTO igual de limpios
    ;
    public abstract double calcularRecargoEnvio(double precio);
    public abstract String generarEtiqueta(String nombre);
}

// ClasificadorProducto.java — sin switch
public double calcularRecargoEnvio(Producto producto) {
    return producto.getTipo().calcularRecargoEnvio(producto.getPrecio());
}

// EtiquetadoProducto.java — sin switch
public String generarEtiqueta(Producto producto) {
    return producto.getTipo().generarEtiqueta(producto.getNombre());
}
```

**Resultado:** cero `switch` en `ClasificadorProducto` ni `EtiquetadoProducto`. Agregar `DEPORTE` = añadir 1 constante al enum.

---

### Resultado de `mvn test` — Parte 1

```
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Pruebas en verde **antes y después** de cada refactorización, sin modificarlas.

---

### Decisión de diseño 1 — Extracción del cálculo de reorden

> **Pregunta:** ¿El cálculo de la cantidad de reorden se extrae como método privado de `InventarioService` o como clase colaboradora `CalculadoraReorden`?

**Decisión:** método privado `calcularCantidadReorden(int stockMinimo, int nuevoStock)` dentro de `InventarioService`.

**Justificación:** la lógica es una expresión aritmética de una línea (`stockMinimo * 3 - nuevoStock`), sin estado propio, sin dependencias externas y sin razón de cambio independiente del resto del servicio. Extraerla a una clase separada añadiría indirección sin ganancia real de cohesión. Si en el futuro la fórmula dependiera de proveedores, temporadas o reglas configurables externas, entonces sí justificaría su propia clase con responsabilidad única y posiblemente inyección de dependencias. Por ahora, un método privado con nombre descriptivo cumple perfectamente el principio de responsabilidad única sin sobré-ingeniería.

---

### Decisión de diseño 2 — Mecanismo de polimorfismo para reemplazar el switch

> **Pregunta:** ¿Enum con métodos abstractos por constante o interfaz Strategy con una clase por tipo?

**Decisión:** enum `TipoProducto` con métodos abstractos implementados por cada constante.

**Justificación:** el conjunto de tipos (`LIBRO`, `ELECTRONICO`, `ROPA`, `ALIMENTO`) es **cerrado y estable** — rara vez se agrega un tipo nuevo en este dominio —, y ninguna constante necesita colaboradores inyectados por Spring. En ese escenario, el enum abstracto elimina el switch duplicado con **cero clases adicionales** y mantiene toda la lógica de tipo en un único lugar. La interfaz Strategy sería la opción correcta si: (a) el conjunto de tipos fuera abierto y extensible en tiempo de ejecución, o (b) cada tipo requiriera servicios inyectados por el contenedor IoC. Como ninguna de esas condiciones aplica aquí, el enum es más simple, más legible y suficiente.

---

## Parte 2 — Diagnóstico y Resolución de Acoplamiento entre Paquetes

### El ciclo detectado

Al agregar el módulo `reportes`, se introdujo una **dependencia cíclica** entre paquetes:

```
inventario  ──importa──▶  reportes
   ▲                          │
   └──────────importa──────────┘
```

- `InventarioService` (paquete `inventario`) importaba `ReporteBajoStockService` (paquete `reportes`)
- `ReporteBajoStockService` (paquete `reportes`) importaba `InventarioService` (paquete `inventario`)

El proyecto **compilaba sin errores** y todas las pruebas de negocio seguían en verde. El problema era de **arquitectura**, no de comportamiento: lo detectó únicamente `ArquitecturaPaquetesTest`.

#### Salida de ArchUnit con el ciclo presente (Commit 7 — rojo)

```
java.lang.AssertionError: Architecture Violation [Priority: MEDIUM] -
Rule 'slices matching 'com.universidad.refactoringu11.(*)..'
should be free of cycles' was violated:
Cycle detected: Slice inventario -> Slice reportes -> Slice inventario
  - com.universidad.refactoringu11.inventario.InventarioService
      imports com.universidad.refactoringu11.reportes.ReporteBajoStockService
  - com.universidad.refactoringu11.reportes.ReporteBajoStockService
      imports com.universidad.refactoringu11.inventario.InventarioService
```

---

### La solución: Inversión de Dependencias

Se extrajo la interfaz `NotificadorBajoStock` **en el paquete `inventario`** (dominio). Ambos paquetes ahora dependen de esa abstracción; reportes implementa el contrato:

#### Antes (con ciclo)
```
inventario ──▶ ReporteBajoStockService (reportes)
reportes   ──▶ InventarioService (inventario)
```

#### Después (sin ciclo, dirección única)
```
inventario  define  NotificadorBajoStock (interfaz)
reportes    ──▶     inventario  (implementa NotificadorBajoStock, usa InventarioService)
```

```java
// inventario/NotificadorBajoStock.java  ← dominio define el contrato
public interface NotificadorBajoStock {
    void notificar(Producto producto, int cantidadReorden);
}

// inventario/InventarioService.java  ← depende de la ABSTRACCIÓN
private NotificadorBajoStock notificador;  // NO importa reportes

// reportes/ReporteBajoStockService.java  ← infraestructura implementa
public class ReporteBajoStockService implements NotificadorBajoStock { ... }
```

#### Salida de ArchUnit tras la corrección (Commit 8 — verde)

```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

#### Suite completa tras reestructuración de paquetes

```
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Las pruebas de caracterización de la Parte 1 se ejecutaron **sin modificar** tras los cambios de Parte 2 y todas siguen en verde.

---

### Decisión de diseño 3 — Ubicación de `NotificadorBajoStock`

> **Pregunta:** ¿Por qué `NotificadorBajoStock` vive en el paquete `inventario` y no en `reportes`?

**Decisión:** la interfaz reside en `inventario` (dominio).

**Justificación:** aplicando la regla de Inversión de Dependencias — "los módulos de infraestructura dependen de dominio; dominio nunca depende de infraestructura" — el paquete de **mayor nivel de abstracción** en este proyecto es `inventario`: contiene las reglas de negocio puras (stock, reorden, movimientos) sin importar cómo se persiste, notifica ni reporta. `reportes` es infraestructura: implementa la notificación en una forma concreta (texto plano, lista en memoria). Al colocar la interfaz en el dominio, el dominio define **qué necesita** (ser notificado de bajo stock) sin saber **cómo** se implementa. `reportes` apunta hacia adentro implementando ese contrato, y el ciclo desaparece por diseño.

---

### Decisión de diseño 4 — Granularidad de `NotificadorBajoStock`

> **Pregunta:** `notificar(Producto, int cantidadReorden)` por evento vs. `notificar(List<Producto>)` por lote. ¿Cuál y por qué?

**Decisión:** notificación por evento individual.

**Justificación:**

| Criterio | Por evento | Por lote |
|---|---|---|
| Reactividad | Inmediata al detectar bajo stock | Diferida hasta fin del lote |
| Simplicidad de implementación | Alta (sin acumulación de estado) | Media (requiere acumular y liberar) |
| Caso de uso | Inventario en tiempo real | Importación masiva (cientos de productos) |
| Costo de notificación | Bajo (println / lista en memoria) | — |

En este proyecto la detección de bajo stock ocurre dentro de un único movimiento de inventario, y el costo de notificación es mínimo (registro en memoria). La notificación por evento es más simple, elimina la necesidad de acumular estado entre llamadas y permite reaccionar en la misma transacción. Se preferiría la notificación por lote si el canal de salida tuviera costo por mensaje (correo, SMS, llamada HTTP) y se procesaran cientos de productos en una sola operación de importación, donde consolidar todas las alertas en un solo envío reduciría costos y latencia.

---

## Historial de commits

| # | Mensaje | Parte |
|---|---|---|
| 1 | `feat: crear inventario con Long Method en InventarioService y switch duplicado` | 1 |
| 2 | `test: agregar pruebas de caracterizacion antes de refactorizar (red de seguridad)` | 1 |
| 3 | `refactor: eliminar Long Method en actualizarInventario descomponiendo responsabilidades` | 1 |
| 4 | `refactor: eliminar switch duplicado sobre TipoProducto con polimorfismo` | 1 |
| 5 | `docs: documentar evidencia de code smells, decisiones de diseno y resultado de pruebas` | 1 |
| 6 | `feat: agregar modulo de reportes con notificacion de bajo stock desde inventario` | 2 |
| 7 | `test: agregar prueba de arquitectura que detecta ciclo entre inventario y reportes (en rojo)` | 2 |
| 8 | `refactor: invertir la dependencia entre inventario y reportes extrayendo NotificadorBajoStock` | 2 |
| 9 | `docs: completar README con diagrama de paquetes antes/despues y decisiones de la Parte 2` | 2 |
