import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Check, CreditCard, Download, Smartphone } from 'lucide-react';
import { useAuth } from '../../auth/AuthContext';
import { downloadComprobante, getMyOrder, payOrder } from './orders.api';
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
  { value: 'DEBIT_CARD', label: 'Tarjeta de débito' },
];

const isCard = (m: PaymentMethod) => m === 'CREDIT_CARD' || m === 'DEBIT_CARD';

const onlyDigits = (s: string) => s.replace(/\D/g, '');
const groupCard = (s: string) => onlyDigits(s).slice(0, 16).replace(/(.{4})/g, '$1 ').trim();
const groupExpiry = (s: string) => {
  const d = onlyDigits(s).slice(0, 4);
  return d.length <= 2 ? d : `${d.slice(0, 2)}/${d.slice(2)}`;
};

interface CardForm { number: string; name: string; expiry: string; cvv: string }
interface YapeForm { phone: string; code: string }

/** Simulación: valida formato de tarjeta (16 díg, titular, MM/AA no vencida, CVV). */
function cardValid(c: CardForm): boolean {
  if (onlyDigits(c.number).length !== 16) return false;
  if (c.name.trim().length < 3) return false;
  const m = c.expiry.match(/^(\d{2})\/(\d{2})$/);
  if (!m) return false;
  const mm = Number(m[1]);
  if (mm < 1 || mm > 12) return false;
  const now = new Date();
  const curYM = now.getFullYear() * 100 + (now.getMonth() + 1);
  const expYM = (2000 + Number(m[2])) * 100 + mm;
  if (expYM < curYM) return false;
  return onlyDigits(c.cvv).length === 3;
}

/** Simulación: celular Yape (9 díg, empieza en 9) + código de aprobación de 6 díg. */
function yapeValid(y: YapeForm): boolean {
  return /^9\d{8}$/.test(onlyDigits(y.phone)) && onlyDigits(y.code).length === 6;
}

/** Detalle de un pedido: comprobante, líneas, desglose de montos y pago si está pendiente. */
export function OrderDetailPage() {
  const { id } = useParams();
  const { isAuthenticated } = useAuth();

  const [order, setOrder] = useState<OrderResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [method, setMethod] = useState<PaymentMethod>('YAPE');
  const [card, setCard] = useState<CardForm>({ number: '', name: '', expiry: '', cvv: '' });
  const [yape, setYape] = useState<YapeForm>({ phone: '', code: '' });
  const [paying, setPaying] = useState(false);
  const [downloading, setDownloading] = useState(false);
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

  const formValid = useMemo(
    () => (isCard(method) ? cardValid(card) : yapeValid(yape)),
    [method, card, yape],
  );

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
  // El comprobante se emite SOLO tras el pago: no existe en PENDIENTE (sin pagar) ni CANCELADA.
  const isPaid = order.status !== 'PENDIENTE' && order.status !== 'CANCELADA';

  const pay = async () => {
    if (!formValid) return;
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

  const descargar = async () => {
    setDownloading(true);
    setError(null);
    try {
      await downloadComprobante(order.id);
    } catch {
      setError('No se pudo descargar el comprobante. Intentá de nuevo.');
    } finally {
      setDownloading(false);
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
          {/* COMPROBANTE — solo disponible una vez pagado el pedido */}
          {isPaid && (
            <div className="od-card od-doc">
              <h2>{isFactura ? 'Factura' : 'Boleta'}</h2>
              <div className="od-doc__row"><span>{isFactura ? 'Razón social' : 'Nombre'}</span><strong>{order.customerName}</strong></div>
              <div className="od-doc__row"><span>{isFactura ? 'RUC' : 'DNI'}</span><strong>{order.customerDoc}</strong></div>
              <button type="button" className="od-doc__dl" onClick={descargar} disabled={downloading}>
                <Download size={16} /> {downloading ? 'Generando…' : 'Descargar comprobante'}
              </button>
            </div>
          )}

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

              {isCard(method) ? (
                <div className="od-form">
                  <label className="od-field">
                    <span>Número de tarjeta</span>
                    <input inputMode="numeric" autoComplete="cc-number" placeholder="0000 0000 0000 0000"
                      value={card.number} onChange={(e) => setCard({ ...card, number: groupCard(e.target.value) })} />
                  </label>
                  <div className="od-field-row">
                    <label className="od-field">
                      <span>Vencimiento</span>
                      <input inputMode="numeric" placeholder="MM/AA"
                        value={card.expiry} onChange={(e) => setCard({ ...card, expiry: groupExpiry(e.target.value) })} />
                    </label>
                    <label className="od-field">
                      <span>CVV</span>
                      <input inputMode="numeric" type="password" placeholder="123"
                        value={card.cvv} onChange={(e) => setCard({ ...card, cvv: onlyDigits(e.target.value).slice(0, 3) })} />
                    </label>
                  </div>
                  <label className="od-field">
                    <span>Titular</span>
                    <input autoComplete="cc-name" placeholder="Como figura en la tarjeta"
                      value={card.name} onChange={(e) => setCard({ ...card, name: e.target.value })} />
                  </label>
                </div>
              ) : (
                <div className="od-form">
                  <label className="od-field">
                    <span>Celular Yape</span>
                    <input inputMode="numeric" placeholder="9XXXXXXXX"
                      value={yape.phone} onChange={(e) => setYape({ ...yape, phone: onlyDigits(e.target.value).slice(0, 9) })} />
                  </label>
                  <label className="od-field">
                    <span>Código de aprobación</span>
                    <input inputMode="numeric" placeholder="6 dígitos"
                      value={yape.code} onChange={(e) => setYape({ ...yape, code: onlyDigits(e.target.value).slice(0, 6) })} />
                  </label>
                  <p className="od-hint">Abrí tu app de Yape, confirmá el pago y copiá el código de aprobación.</p>
                </div>
              )}

              <button type="button" className="od-pay__btn" onClick={pay} disabled={paying || !formValid}>
                {isCard(method) ? <CreditCard size={18} /> : <Smartphone size={18} />}
                {paying ? 'Procesando…' : `Pagar ${pen.format(order.total)}`}
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
