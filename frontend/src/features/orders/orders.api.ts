import { api } from '../../lib/api';
import type {
  CheckoutRequest, OrderResponse, PaymentRequest,
} from '../../models/order';

/** POST /api/orders/checkout — crea el pedido desde el carrito (201). */
export async function checkout(body: CheckoutRequest): Promise<OrderResponse> {
  const { data } = await api.post<OrderResponse>('/api/orders/checkout', body);
  return data;
}

/** GET /api/orders — pedidos del usuario, más nuevos primero. */
export async function getMyOrders(): Promise<OrderResponse[]> {
  const { data } = await api.get<OrderResponse[]>('/api/orders');
  return data;
}

/** GET /api/orders/{id} — detalle de un pedido propio (404 si no es el dueño). */
export async function getMyOrder(id: number): Promise<OrderResponse> {
  const { data } = await api.get<OrderResponse>(`/api/orders/${id}`);
  return data;
}

/** POST /api/orders/{id}/pay — pago simulado: PENDIENTE → CONFIRMADA. */
export async function payOrder(id: number, body: PaymentRequest): Promise<OrderResponse> {
  const { data } = await api.post<OrderResponse>(`/api/orders/${id}/pay`, body);
  return data;
}
