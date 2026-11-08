package com.universidad.refactoringu11.inventario;

import com.universidad.refactoringu11.reportes.ReporteBajoStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class InventarioService {

    private final List<Producto> productos = new ArrayList<>();
    private final List<String> movimientos = new ArrayList<>();

    // inventario importa reportes -- introduce el ciclo
    private ReporteBajoStockService reporteBajoStockService;

    public void agregarProducto(Producto producto) {
        productos.add(producto);
    }

    @Autowired(required = false)
    public void setReporteBajoStockService(ReporteBajoStockService r) {
        this.reporteBajoStockService = r;
    }

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

    private boolean esEntradaValida(Long productoId, int cantidad, String tipoMovimiento) {
        return productoId != null
                && cantidad != 0
                && (tipoMovimiento.equals("ENTRADA") || tipoMovimiento.equals("SALIDA"));
    }

    private Producto buscarProducto(Long productoId) {
        return productos.stream()
                .filter(p -> p.getId().equals(productoId))
                .findFirst()
                .orElse(null);
    }

    private String aplicarMovimiento(Producto producto, int cantidad, String tipoMovimiento) {
        int nuevoStock;
        if (tipoMovimiento.equals("ENTRADA")) {
            nuevoStock = producto.getStock() + cantidad;
        } else {
            nuevoStock = producto.getStock() - cantidad;
            if (nuevoStock < 0) return "ERROR_STOCK_INSUFICIENTE";
        }
        producto.setStock(nuevoStock);
        return null;
    }

    private void registrarMovimiento(Producto producto, int cantidad,
                                     String tipoMovimiento, int nuevoStock) {
        String registro = tipoMovimiento + " de " + cantidad + " unidades de "
                + producto.getNombre() + " -> stock final: " + nuevoStock;
        movimientos.add(registro);
        System.out.println(registro);
    }

    private String evaluarReorden(Producto producto, int nuevoStock) {
        boolean necesitaReorden = nuevoStock < producto.getStockMinimo();
        if (!necesitaReorden) return "OK_" + nuevoStock;
        int cantidadReorden = calcularCantidadReorden(producto.getStockMinimo(), nuevoStock);
        if (reporteBajoStockService != null) {
            System.out.println(reporteBajoStockService.generarReporte());
        }
        System.out.println("ALERTA: " + producto.getNombre()
                + " bajo stock minimo. Reordenar " + cantidadReorden + " unidades.");
        return "OK_BAJO_STOCK_REORDEN_" + cantidadReorden;
    }

    private int calcularCantidadReorden(int stockMinimo, int nuevoStock) {
        return (stockMinimo * 3) - nuevoStock;
    }

    public List<String> getMovimientos() { return movimientos; }
}
