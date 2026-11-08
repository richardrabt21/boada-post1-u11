# boada-post1-u11 â€” RefactorizaciÃ³n Guiada por Pruebas

**Curso:** Patrones de DiseÃ±o de Software Â· Unidad 11  
**Estudiante:** Richard Boada  
**Repositorio:** https://github.com/richardrabt21/boada-post1-u11

> Post-contenido â€” RefactorizaciÃ³n Guiada por Pruebas: Code Smells, Paquetes y Acoplamiento en un Proyecto Java Real

---

## Estructura del proyecto

```
refactoring-u11/
â”œâ”€â”€ pom.xml
â””â”€â”€ src/
    â”œâ”€â”€ main/java/com/universidad/refactoringu11/
    â”‚   â”œâ”€â”€ RefactoringU11Application.java
    â”‚   â”œâ”€â”€ inventario/
    â”‚   â”‚   â”œâ”€â”€ TipoProducto.java          â† enum con polimorfismo (Parte 1)
    â”‚   â”‚   â”œâ”€â”€ Producto.java
    â”‚   â”‚   â”œâ”€â”€ InventarioService.java     â† Long Method eliminado (Parte 1)
    â”‚   â”‚   â”œâ”€â”€ ClasificadorProducto.java  â† Switch eliminado (Parte 1)
    â”‚   â”‚   â”œâ”€â”€ EtiquetadoProducto.java    â† Switch eliminado (Parte 1)
    â”‚   â”‚   â””â”€â”€ NotificadorBajoStock.java  â† interfaz DIP (Parte 2)
    â”‚   â””â”€â”€ reportes/
    â”‚       â””â”€â”€ ReporteBajoStockService.java â† implementa NotificadorBajoStock
    â””â”€â”€ test/java/com/universidad/refactoringu11/
        â”œâ”€â”€ inventario/
        â”‚   â”œâ”€â”€ InventarioServiceTest.java
        â”‚   â”œâ”€â”€ ClasificadorProductoTest.java
        â”‚   â””â”€â”€ EtiquetadoProductoTest.java
        â””â”€â”€ ArquitecturaPaquetesTest.java
```

---

## Parte 1 â€” Red de Seguridad y RefactorizaciÃ³n Guiada por Evidencia

### Code Smell 1: Long Method en `actualizarInventario()`

#### Antes de refactorizar

El mÃ©todo `actualizarInventario()` tenÃ­a **52 lÃ­neas** y mezclaba **5 responsabilidades** en un Ãºnico bloque:

| # | Responsabilidad | LÃ­neas aprox. |
|---|---|---|
| 1 | ValidaciÃ³n de entrada | 4 |
| 2 | BÃºsqueda del producto | 5 |
| 3 | ActualizaciÃ³n de stock | 8 |
| 4 | CÃ¡lculo de cantidad de reorden | 3 |
| 5 | Registro/alerta | 10 |

```java
// ANTES â€” un solo mÃ©todo de >35 lÃ­neas mezclando 5 responsabilidades
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

#### DespuÃ©s de refactorizar (tÃ©cnica: *Extract Method* â€” Fowler, cap. 6)

```java
// DESPUÃ‰S â€” mÃ©todo principal de 10 lÃ­neas, delega en mÃ©todos privados nombrados
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

**Resultado:** `actualizarInventario()` pasÃ³ de **52 â†’ 10 lÃ­neas**. Cada responsabilidad vive en su propio mÃ©todo privado con nombre que revela intenciÃ³n.

---

### Code Smell 2: Switch Statements duplicados sobre `TipoProducto`

#### Antes de refactorizar

El mismo `switch` sobre `TipoProducto` con las 4 ramas idÃ©nticas aparecÃ­a en dos clases distintas:

```java
// ClasificadorProducto.java â€” switch #1
public double calcularRecargoEnvio(Producto producto) {
    switch (producto.getTipo()) {
        case LIBRO:       return 0.0;
        case ELECTRONICO: return producto.getPrecio() * 0.08;
        case ROPA:        return producto.getPrecio() * 0.03;
        case ALIMENTO:    return 2.50;
        default: throw new IllegalArgumentException("...");
    }
}

// EtiquetadoProducto.java â€” switch #2 (mismo esqueleto, distinta lÃ³gica)
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

#### DespuÃ©s de refactorizar (tÃ©cnica: *Replace Conditional with Polymorphism* â€” Fowler, cap. 10)

```java
// TipoProducto.java â€” cada constante implementa su propio comportamiento
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

// ClasificadorProducto.java â€” sin switch
public double calcularRecargoEnvio(Producto producto) {
    return producto.getTipo().calcularRecargoEnvio(producto.getPrecio());
}

// EtiquetadoProducto.java â€” sin switch
public String generarEtiqueta(Producto producto) {
    return producto.getTipo().generarEtiqueta(producto.getNombre());
}
```

**Resultado:** cero `switch` en `ClasificadorProducto` ni `EtiquetadoProducto`. Agregar `DEPORTE` = aÃ±adir 1 constante al enum.

---

### Resultado de `mvn test` â€” Parte 1

```
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Pruebas en verde **antes y despuÃ©s** de cada refactorizaciÃ³n, sin modificarlas.

---

### DecisiÃ³n de diseÃ±o 1 â€” ExtracciÃ³n del cÃ¡lculo de reorden

> **Pregunta:** Â¿El cÃ¡lculo de la cantidad de reorden se extrae como mÃ©todo privado de `InventarioService` o como clase colaboradora `CalculadoraReorden`?

**DecisiÃ³n:** mÃ©todo privado `calcularCantidadReorden(int stockMinimo, int nuevoStock)` dentro de `InventarioService`.

**JustificaciÃ³n:** la lÃ³gica es una expresiÃ³n aritmÃ©tica de una lÃ­nea (`stockMinimo * 3 - nuevoStock`), sin estado propio, sin dependencias externas y sin razÃ³n de cambio independiente del resto del servicio. Extraerla a una clase separada aÃ±adirÃ­a indirecciÃ³n sin ganancia real de cohesiÃ³n. Si en el futuro la fÃ³rmula dependiera de proveedores, temporadas o reglas configurables externas, entonces sÃ­ justificarÃ­a su propia clase con responsabilidad Ãºnica y posiblemente inyecciÃ³n de dependencias. Por ahora, un mÃ©todo privado con nombre descriptivo cumple perfectamente el principio de responsabilidad Ãºnica sin sobrÃ©-ingenierÃ­a.

---

### DecisiÃ³n de diseÃ±o 2 â€” Mecanismo de polimorfismo para reemplazar el switch

> **Pregunta:** Â¿Enum con mÃ©todos abstractos por constante o interfaz Strategy con una clase por tipo?

**DecisiÃ³n:** enum `TipoProducto` con mÃ©todos abstractos implementados por cada constante.

**JustificaciÃ³n:** el conjunto de tipos (`LIBRO`, `ELECTRONICO`, `ROPA`, `ALIMENTO`) es **cerrado y estable** â€” rara vez se agrega un tipo nuevo en este dominio â€”, y ninguna constante necesita colaboradores inyectados por Spring. En ese escenario, el enum abstracto elimina el switch duplicado con **cero clases adicionales** y mantiene toda la lÃ³gica de tipo en un Ãºnico lugar. La interfaz Strategy serÃ­a la opciÃ³n correcta si: (a) el conjunto de tipos fuera abierto y extensible en tiempo de ejecuciÃ³n, o (b) cada tipo requiriera servicios inyectados por el contenedor IoC. Como ninguna de esas condiciones aplica aquÃ­, el enum es mÃ¡s simple, mÃ¡s legible y suficiente.

---

