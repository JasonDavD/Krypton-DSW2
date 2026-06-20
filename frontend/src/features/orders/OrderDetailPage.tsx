import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Check, CreditCard } from 'lucide-react';
import { useAuth } from '../../auth/AuthContext';
import { getMyOrder, payOrder } from './orders.api';
import type { OrderResponse, OrderStatus, PaymentMethod } from '../../models/order';
import './orders.css';
import './order-detail.css';

const pen = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN', minimumFractionDigits: 2 });
const dateFmt = new Intl.DateTimeFormat('es-PE', { dateStyle: 'long', timeStyle: 'short' });

const STATUS_LABEL: Record<OrderStatus, string> = {
  PENDIENTE: 'Pendiente de pago', CONFIRMADA: 'Confirmada',
  ENVIADO: 'Enviado', ENTREGADO: 'Entregado', CANCELADA: 'Cancelada',
};
const PAY_METHODS: { value: PaymentMethod; label: string }[] = [
  { value: 'YAPE', label: 'Yape' },
  { value: 'CREDIT_CARD', label: 'Tarjeta de crédito' },
  { value: 'EFECTIVO', label: 'Efectivo' },
];

/** Detalle de un pedido: comprobante, líneas, desglose de montos y pago si está pendiente. */
export function OrderDetailPage() {
  const { id } = useParams();
  const { isAuthenticated } = useAuth();

  const [order, setOrder] = useState<OrderResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [method, setMethod] = useState<PaymentMethod>('YAPE');
  const [paying, setPaying] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isAuthenticated) { setLoading(false); return; }
    const orderId = Number(id);
    if (!Number.isFinite(orderId)) { setNotFound(true); setLoading(false); return; }
    getMyOrder(orderId)
      .then(setOrder)
      .catch(() => setNotFound(true))
      .finally(() => setLoading(false));
  }, [id, isAuthenticated]);

  if (!isAuthenticated) {
    return <div className="ord ord-gate"><h1>Iniciá sesión para ver el pedido</h1>
      <Link to="/cuenta/ingresar" className="ord-gate__cta">Iniciar sesión</Link></div>;
  }
  if (loading) return <div className="ord"><p className="ord-status">Cargando pedido…</p></div>;
  if (notFound || !order) {
    return (
      <div className="ord ord-empty">
        <h1>Pedido no encontrado</h1>
        <p>No existe o no te pertenece.</p>
        <Link to="/pedidos" className="ord-empty__cta">Volver a mis pedidos</Link>
      </div>
    );
  }

  const base = order.total - order.igv; // base imponible = total − IGV
  const isFactura = order.documentType === 'FACTURA';

  const pay = async () => {
    setPaying(true);
    setError(null);
    try {
      setOrder(await payOrder(order.id, { method }));
    } catch {
      setError('No se pudo procesar el pago. Intentá de nuevo.');
    } finally {
      setPaying(false);
    }
  };

  return (
    <div className="ord od">
      <nav className="od-crumb"><Link to="/pedidos">Mis pedidos</Link><span>/</span><strong>Pedido #{order.id}</strong></nav>

      <header className="od-head">
        <div>
          <h1>Pedido #{order.id}</h1>
          <span className="od-date">{dateFmt.format(new Date(order.orderDate))}</span>
        </div>
        <span className={`ord-badge ord-badge--${order.status.toLowerCase()}`}>{STATUS_LABEL[order.status]}</span>
      </header>

      <div className="od-grid">
        <section className="od-main">
          {/* COMPROBANTE */}
          <div className="od-card od-doc">
            <h2>{isFactura ? 'Factura' : 'Boleta'}</h2>
            <div className="od-doc__row"><span>{isFactura ? 'Razón social' : 'Nombre'}</span><strong>{order.customerName}</strong></div>
            <div className="od-doc__row"><span>{isFactura ? 'RUC' : 'DNI'}</span><strong>{order.customerDoc}</strong></div>
          </div>

          {/* LÍNEAS */}
          <div className="od-card">
            <h2>Productos</h2>
            <ul className="od-items">
              {order.items.map((it) => (
                <li key={it.id} className="od-item">
                  <Link to={`/catalogo/${it.productId}`} className="od-item__name">{it.productName}</Link>
                  <span className="od-item__qty">{it.quantity} × {pen.format(it.unitPrice)}</span>
                  <span className="od-item__sub">{pen.format(it.subtotal)}</span>
                </li>
              ))}
            </ul>
          </div>
        </section>

        {/* RESUMEN + PAGO */}
        <aside className="od-side">
          <div className="od-card">
            <h2>Resumen</h2>
            <div className="od-row"><span>Subtotal</span><span>{pen.format(order.subtotal)}</span></div>
            <div className="od-row"><span>Envío</span><span>{order.shippingCost === 0 ? 'Gratis' : pen.format(order.shippingCost)}</span></div>
            {isFactura && (
              <>
                <div className="od-row od-row--muted"><span>Op. gravada</span><span>{pen.format(base)}</span></div>
                <div className="od-row od-row--muted"><span>IGV (18%)</span><span>{pen.format(order.igv)}</span></div>
              </>
            )}
            {!isFactura && (
              <div className="od-row od-row--muted"><span>IGV incluido</span><span>{pen.format(order.igv)}</span></div>
            )}
            <div className="od-total"><span>Total</span><span>{pen.format(order.total)}</span></div>
          </div>

          {order.status === 'PENDIENTE' && (
            <div className="od-card od-pay">
              <h2>Pagar</h2>
              {error && <p className="ord-status od-pay__err">{error}</p>}
              <div className="od-methods">
                {PAY_METHODS.map((m) => (
                  <button
                    key={m.value}
                    type="button"
                    className={method === m.value ? 'od-method od-method--on' : 'od-method'}
                    onClick={() => setMethod(m.value)}
                  >
                    {m.label}
                  </button>
                ))}
              </div>
              <button type="button" className="od-pay__btn" onClick={pay} disabled={paying}>
                <CreditCard size={18} /> {paying ? 'Procesando…' : `Pagar ${pen.format(order.total)}`}
              </button>
            </div>
          )}

          {order.status === 'CONFIRMADA' && (
            <div className="od-card od-paid"><Check size={18} /> Pago confirmado</div>
          )}
        </aside>
      </div>
    </div>
  );
}
