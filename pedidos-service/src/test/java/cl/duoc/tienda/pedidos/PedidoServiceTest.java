package cl.duoc.tienda.pedidos;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PedidoServiceTest {
    private final PedidoRepository repository = Mockito.mock(PedidoRepository.class);
    private final ProductoClient client = Mockito.mock(ProductoClient.class);
    private final PedidoService service = new PedidoService(repository, client);

    @Test
    void calculaElTotalConElPrecioDelCatalogo() {
        when(client.buscar(1L)).thenReturn(new ProductoClient.ProductoDto(1L, "Teclado", 25000, 10));
        when(repository.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals(50000, service.crear("ana", 1L, 2).getTotal());
    }

    @Test
    void rechazaPedidoSinStockSuficiente() {
        when(client.buscar(1L)).thenReturn(new ProductoClient.ProductoDto(1L, "Teclado", 25000, 1));

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.crear("ana", 1L, 2));
        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        verify(repository, never()).save(any());
    }
}
