-- Manual current-period payment marker; this is not a payment transaction.
ALTER TABLE debt ADD COLUMN payment_marked_on DATE;
