package com.facilcomanda.erp.service;

import com.facilcomanda.erp.event.OrderCreatedEvent;
import com.facilcomanda.erp.printer.EscPosReceiptFormatter;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EscPosTcpPrinter {

  private static final Logger log = LoggerFactory.getLogger(EscPosTcpPrinter.class);
  private static final int PRINTER_PORT = 9100;
  private static final int TIMEOUT_MS = 3000;

  private final String printerIp;
  private final EscPosReceiptFormatter formatter;

  public EscPosTcpPrinter(
      @Value("${printer.bar.ip:192.168.100.250}") String printerIp,
      EscPosReceiptFormatter formatter) {
    this.printerIp = printerIp;
    this.formatter = formatter;
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderCreated(OrderCreatedEvent event) {
    byte[] payload = formatter.formatPreCuenta(event.order());
    sendToNetworkPrinter(payload, event.order().getId());
  }

  private void sendToNetworkPrinter(byte[] payload, Long orderId) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(printerIp, PRINTER_PORT), TIMEOUT_MS);

      OutputStream out = socket.getOutputStream();
      out.write(payload);
      out.flush();

      log.info("Orden {} enviada exitosamente a impresora {}", orderId, printerIp);
    } catch (Exception e) {
      log.error(
          "Fallo de red I/O hacia la impresora {} en orden {}: {}",
          printerIp,
          orderId,
          e.getMessage());
    }
  }
}