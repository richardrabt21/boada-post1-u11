package com.universidad.refactoringu11.inventario;

/**
 * Puerto de salida que el dominio (inventario) usa para notificar bajo stock.
 *
 * DECISIÓN DE DISEÑO 3: La interfaz reside en el paquete inventario (dominio)
 * y NO en reportes (infraestructura).
 *
 * Justificación: según el principio de Inversión de Dependencias, los módulos
 * de mayor nivel de abstracción no deben depender de los de menor nivel; ambos
 * deben depender de abstracciones, y esas abstracciones pertenecen al nivel
 * más alto. En este proyecto "inventario" es el dominio (negocio puro) y
 * "reportes" es infraestructura (implementación concreta de notificación).
 * Al colocar NotificadorBajoStock en inventario, el dominio define el contrato
 * que necesita sin importar nada de reportes; reportes implementa ese contrato
 * apuntando hacia adentro (reportes → inventario), y el ciclo desaparece.
 *
 * DECISIÓN DE DISEÑO 4: El método recibe (Producto, int cantidadReorden) por
 * evento individual, no por lote (List<Producto>).
 *
 * Justificación: la notificación por evento permite reaccionar inmediatamente
 * al detectar el bajo stock dentro de la misma transacción de movimiento, sin
 * acumular estado ni esperar a que termine un lote. Es más simple y suficiente
 * en un inventario en tiempo real. La notificación por lote sería preferible
 * si hubiera importaciones masivas donde el costo de notificar individualmente
 * (p. ej., enviar un correo por producto) fuera prohibitivo, o si se quisiera
 * consolidar varias alertas en un solo reporte al final del proceso.
 */
public interface NotificadorBajoStock {
    void notificar(Producto producto, int cantidadReorden);
}
