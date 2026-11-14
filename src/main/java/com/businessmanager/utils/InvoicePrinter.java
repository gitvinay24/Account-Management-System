package com.businessmanager.utils;

import com.businessmanager.model.Business;
import com.businessmanager.model.Sale;
import com.businessmanager.model.SaleItem;
import com.businessmanager.theme.Colors;
import com.businessmanager.theme.Fonts;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.math.BigDecimal;
import java.math.RoundingMode;
import javax.print.PrintService;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Destination;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Sends a completed POS invoice to the operating system's configured printer and provides a live preview. */
public final class InvoicePrinter implements Printable {
    private final Sale sale;
    private final Business business;

    private InvoicePrinter(Sale sale, Business business) {
        this.sale = sale;
        this.business = business;
    }

    public static boolean print(Sale sale, Business business) throws PrinterException {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Invoice " + (sale.getInvoiceNumber() != null ? sale.getInvoiceNumber() : "POS"));
        job.setPrintable(new InvoicePrinter(sale, business));
        if (job.printDialog()) {
            job.print();
            return true;
        }
        return false;
    }

    public static void printToService(Sale sale, Business business, PrintService service, PrintRequestAttributeSet attributes) throws PrinterException {
        Destination destination = attributes == null ? null : (Destination) attributes.get(Destination.class);
        if (destination != null && isPdfService(service)) {
            exportPdf(sale, business, destination.getURI());
            return;
        }

        PrinterJob job = PrinterJob.getPrinterJob();
        if (service != null) {
            job.setPrintService(service);
        }
        job.setJobName("Invoice " + (sale.getInvoiceNumber() != null ? sale.getInvoiceNumber() : "POS"));
        job.setPrintable(new InvoicePrinter(sale, business));
        if (attributes != null) {
            job.print(attributes);
        } else {
            job.print();
        }
    }

    /**
     * Windows' Microsoft Print to PDF driver commonly ignores Java's Destination
     * attribute and opens a native Save As prompt instead.  When the caller has
     * explicitly supplied a PDF destination, write a self-contained invoice PDF
     * there so exports are reliable in both attended and automated workflows.
     */
    private static void exportPdf(Sale sale, Business business, URI destination) throws PrinterException {
        try {
            Path output = Path.of(destination);
            Path parent = output.getParent();
            if (parent != null) Files.createDirectories(parent);

            List<String> lines = invoiceLines(sale, business);
            StringBuilder content = new StringBuilder("BT\n/F1 12 Tf\n50 790 Td\n");
            for (int i = 0; i < lines.size(); i++) {
                if (i > 0) content.append("0 -16 Td\n");
                content.append('(').append(pdfEscape(lines.get(i))).append(") Tj\n");
            }
            content.append("ET\n");

            byte[] stream = content.toString().getBytes(StandardCharsets.ISO_8859_1);
            List<byte[]> objects = new ArrayList<>();
            objects.add("<< /Type /Catalog /Pages 2 0 R >>".getBytes(StandardCharsets.ISO_8859_1));
            objects.add("<< /Type /Pages /Kids [3 0 R] /Count 1 >>".getBytes(StandardCharsets.ISO_8859_1));
            objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>".getBytes(StandardCharsets.ISO_8859_1));
            objects.add(("<< /Length " + stream.length + " >>\nstream\n" + content + "endstream").getBytes(StandardCharsets.ISO_8859_1));
            objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>".getBytes(StandardCharsets.ISO_8859_1));

            java.io.ByteArrayOutputStream pdf = new java.io.ByteArrayOutputStream();
            pdf.write("%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n".getBytes(StandardCharsets.ISO_8859_1));
            List<Integer> offsets = new ArrayList<>();
            offsets.add(0);
            for (int i = 0; i < objects.size(); i++) {
                offsets.add(pdf.size());
                pdf.write((i + 1 + " 0 obj\n").getBytes(StandardCharsets.ISO_8859_1));
                pdf.write(objects.get(i));
                pdf.write("\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
            }
            int xref = pdf.size();
            pdf.write(("xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n").getBytes(StandardCharsets.ISO_8859_1));
            for (int offset : offsets.subList(1, offsets.size())) {
                pdf.write((String.format("%010d 00000 n \n", offset)).getBytes(StandardCharsets.ISO_8859_1));
            }
            pdf.write(("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n").getBytes(StandardCharsets.ISO_8859_1));
            Files.write(output, pdf.toByteArray());
        } catch (IOException | IllegalArgumentException ex) {
            PrinterException error = new PrinterException("Unable to write invoice PDF: " + ex.getMessage());
            error.initCause(ex);
            throw error;
        }
    }

    private static boolean isPdfService(PrintService service) {
        return service != null && service.getName() != null && service.getName().toLowerCase(java.util.Locale.ROOT).contains("pdf");
    }

    private static List<String> invoiceLines(Sale sale, Business business) {
        List<String> lines = new ArrayList<>();
        lines.add(business != null && business.getName() != null ? business.getName() : "Account Management");
        lines.add("Invoice: " + (sale.getInvoiceNumber() == null ? "" : sale.getInvoiceNumber()));
        lines.add("Date: " + (sale.getSaleDate() == null ? "" : sale.getSaleDate()));
        lines.add("------------------------------------------------------------");
        if (sale.getItems() != null) {
            for (SaleItem item : sale.getItems()) {
                String name = item.getProductName() == null ? "Product #" + item.getProductId() : item.getProductName();
                lines.add(trim(name, 42) + "  " + item.getQuantity() + " x " + money(item.getUnitPrice()));
                lines.add("  " + money(item.getLineTotal()));
            }
        }
        lines.add("------------------------------------------------------------");
        BigDecimal subtotal = sale.getSubtotal() != null ? sale.getSubtotal() : (sale.getTotal() != null ? sale.getTotal() : BigDecimal.ZERO);
        BigDecimal discount = sale.getDiscount() != null ? sale.getDiscount() : BigDecimal.ZERO;
        BigDecimal tax = sale.getTax() != null ? sale.getTax() : BigDecimal.ZERO;
        BigDecimal total = sale.getTotal() != null ? sale.getTotal() : subtotal;
        BigDecimal paid = sale.getPaidAmount() != null ? sale.getPaidAmount() : total;
        BigDecimal change = sale.getChangeAmount() != null ? sale.getChangeAmount() : BigDecimal.ZERO;
        BigDecimal tendered = paid.add(change);
        BigDecimal taxable = subtotal.subtract(discount).max(BigDecimal.ZERO);

        lines.add("Subtotal: " + money(subtotal));
        if (sale.getCouponCode() != null && !sale.getCouponCode().isBlank()) {
            lines.add("Offer Applied (" + sale.getCouponCode() + "): -" + money(discount));
        } else if (discount.signum() > 0) {
            lines.add("Discount: -" + money(discount));
        }
        lines.add("Taxable Amount: " + money(taxable));
        String gstLbl = sale.getGstRate() != null && sale.getGstRate().signum() > 0 ? "GST (" + sale.getGstRate().setScale(0, RoundingMode.HALF_UP) + "%): " : "GST: ";
        lines.add(gstLbl + money(tax));
        lines.add("GRAND TOTAL: " + money(total));
        lines.add("Paid: " + money(tendered) + "   Change: " + money(change));
        lines.add("------------------------------------------------------------");
        lines.add("Thank you for your business.");
        return lines;
    }

    private static String pdfEscape(String value) {
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
                .replaceAll("[^\\x20-\\x7E]", "?");
    }

    public static void showPreview(Component parent, Sale sale, Business business) {
        Window window = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog;
        if (window instanceof Frame frame) {
            dialog = new JDialog(frame, "Invoice Preview - " + sale.getInvoiceNumber(), true);
        } else if (window instanceof Dialog d) {
            dialog = new JDialog(d, "Invoice Preview - " + sale.getInvoiceNumber(), true);
        } else {
            dialog = new JDialog((Frame) null, "Invoice Preview - " + sale.getInvoiceNumber(), true);
        }

        dialog.setLayout(new BorderLayout());
        dialog.setSize(500, 680);
        dialog.setLocationRelativeTo(parent);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 15));
        mainPanel.setBackground(Colors.BACKGROUND);
        mainPanel.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Invoice Receipt Container
        JPanel receipt = new JPanel();
        receipt.setLayout(new BoxLayout(receipt, BoxLayout.Y_AXIS));
        receipt.setBackground(Color.WHITE);
        receipt.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 230), 1),
                new EmptyBorder(24, 24, 24, 24)
        ));

        // Header Section
        String bizName = business != null && business.getName() != null ? business.getName() : "Account Management";
        JLabel titleLbl = new JLabel(bizName, SwingConstants.CENTER);
        titleLbl.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        titleLbl.setForeground(new Color(20, 30, 45));
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        String branchText = "Branch: " + (sale.getBranchName() != null ? sale.getBranchName() : "Main Branch");
        JLabel branchLbl = new JLabel(branchText, SwingConstants.CENTER);
        branchLbl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        branchLbl.setForeground(Color.GRAY);
        branchLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        receipt.add(titleLbl);
        receipt.add(Box.createVerticalStrut(4));
        receipt.add(branchLbl);
        receipt.add(Box.createVerticalStrut(12));
        receipt.add(createDivider());
        receipt.add(Box.createVerticalStrut(12));

        // Metadata Grid (Invoice #, Date, Customer, Cashier)
        JPanel metaGrid = new JPanel(new GridLayout(4, 2, 8, 4));
        metaGrid.setOpaque(false);
        metaGrid.setMaximumSize(new Dimension(1000, 80));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String dateStr = sale.getSaleDate() != null ? sdf.format(sale.getSaleDate()) : sdf.format(new Date());

        metaGrid.add(createMetaLabel("Invoice #:", true));
        metaGrid.add(createMetaLabel(sale.getInvoiceNumber() != null ? sale.getInvoiceNumber() : "N/A", false));
        metaGrid.add(createMetaLabel("Date:", true));
        metaGrid.add(createMetaLabel(dateStr, false));
        metaGrid.add(createMetaLabel("Customer:", true));
        metaGrid.add(createMetaLabel(sale.getCustomerName() != null ? sale.getCustomerName() : "Walk-in Customer", false));
        metaGrid.add(createMetaLabel("Cashier:", true));
        metaGrid.add(createMetaLabel(sale.getUserName() != null ? sale.getUserName() : "Admin", false));

        receipt.add(metaGrid);
        receipt.add(Box.createVerticalStrut(12));
        receipt.add(createDivider());
        receipt.add(Box.createVerticalStrut(12));

        // Items Table
        String[] cols = {"PRODUCT", "QTY", "RATE", "TOTAL"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        if (sale.getItems() != null) {
            for (SaleItem item : sale.getItems()) {
                String name = item.getProductName() != null ? item.getProductName() : "Product #" + item.getProductId();
                model.addRow(new Object[]{
                        name,
                        item.getQuantity(),
                        formatMoney(item.getUnitPrice()),
                        formatMoney(item.getLineTotal())
                });
            }
        }

        JTable table = new JTable(model);
        table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        table.setRowHeight(26);
        table.setShowGrid(false);
        table.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        table.getTableHeader().setBackground(new Color(245, 247, 250));
        table.setBackground(Color.WHITE);

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setOpaque(false);
        tableScroll.getViewport().setBackground(Color.WHITE);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.setPreferredSize(new Dimension(420, 160));

        receipt.add(tableScroll);
        receipt.add(Box.createVerticalStrut(12));
        receipt.add(createDivider());
        receipt.add(Box.createVerticalStrut(12));

        // Calculation Totals Breakdown
        JPanel totalsGrid = new JPanel(new GridLayout(0, 2, 8, 4));
        totalsGrid.setOpaque(false);
        totalsGrid.setMaximumSize(new Dimension(1000, 160));

        BigDecimal subtotal = sale.getSubtotal() != null ? sale.getSubtotal() : (sale.getTotal() != null ? sale.getTotal() : BigDecimal.ZERO);
        BigDecimal discount = sale.getDiscount() != null ? sale.getDiscount() : BigDecimal.ZERO;
        BigDecimal tax = sale.getTax() != null ? sale.getTax() : BigDecimal.ZERO;
        BigDecimal total = sale.getTotal() != null ? sale.getTotal() : subtotal;
        BigDecimal paid = sale.getPaidAmount() != null ? sale.getPaidAmount() : total;
        BigDecimal change = sale.getChangeAmount() != null ? sale.getChangeAmount() : BigDecimal.ZERO;
        BigDecimal tendered = paid.add(change);
        BigDecimal taxable = subtotal.subtract(discount).max(BigDecimal.ZERO);

        totalsGrid.add(createMetaLabel("Subtotal:", true));
        totalsGrid.add(createMetaLabel(formatMoney(subtotal), false));

        if (sale.getCouponCode() != null && !sale.getCouponCode().isBlank()) {
            totalsGrid.add(createMetaLabel("Offer Applied (" + sale.getCouponCode() + "):", true));
            totalsGrid.add(createMetaLabel("- " + formatMoney(discount), false));
        } else if (discount.signum() > 0) {
            totalsGrid.add(createMetaLabel("Discount:", true));
            totalsGrid.add(createMetaLabel("- " + formatMoney(discount), false));
        }

        totalsGrid.add(createMetaLabel("Taxable Amount:", true));
        totalsGrid.add(createMetaLabel(formatMoney(taxable), false));

        String gstLabel = "GST:";
        if (sale.getGstRate() != null && sale.getGstRate().signum() > 0) {
            gstLabel = "GST (" + sale.getGstRate().setScale(0, RoundingMode.HALF_UP) + "%):";
        }
        totalsGrid.add(createMetaLabel(gstLabel, true));
        totalsGrid.add(createMetaLabel(formatMoney(tax), false));

        totalsGrid.add(createBoldMetaLabel("Grand Total:", true));
        totalsGrid.add(createBoldMetaLabel(formatMoney(total), false));

        totalsGrid.add(createMetaLabel("Payment Received (" + (sale.getPaymentMethod() != null ? sale.getPaymentMethod().name() : "CASH") + "):", true));
        totalsGrid.add(createMetaLabel(formatMoney(tendered), false));

        totalsGrid.add(createMetaLabel("Change Returned:", true));
        totalsGrid.add(createMetaLabel(formatMoney(change), false));

        receipt.add(totalsGrid);
        receipt.add(Box.createVerticalStrut(16));

        JLabel footerLbl = new JLabel("Thank you for your business!", SwingConstants.CENTER);
        footerLbl.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
        footerLbl.setForeground(Color.GRAY);
        footerLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        receipt.add(footerLbl);

        JScrollPane mainScroll = new JScrollPane(receipt);
        mainScroll.setBorder(null);
        mainPanel.add(mainScroll, BorderLayout.CENTER);

        // Action Buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);

        JButton printBtn = new JButton("Print Invoice");
        printBtn.setFont(Fonts.TITLE_SM);
        printBtn.setForeground(Colors.ON_PRIMARY);
        printBtn.setBackground(Colors.PRIMARY);
        printBtn.setFocusPainted(false);
        printBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        printBtn.addActionListener(e -> {
            try {
                print(sale, business != null ? business : new Business(-1, "Account Management", null, null));
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(dialog, "Printer notice: " + ex.getMessage(), "Print Info", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        JButton closeBtn = new JButton("Close");
        closeBtn.setFont(Fonts.BODY_SMALL);
        closeBtn.setForeground(Colors.ON_SURFACE);
        closeBtn.setBackground(Colors.SURFACE_CONTAINER_HIGH);
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dialog.dispose());

        actions.add(closeBtn);
        actions.add(printBtn);

        mainPanel.add(actions, BorderLayout.SOUTH);

        dialog.setContentPane(mainPanel);
        dialog.setVisible(true);
    }

    private static JComponent createDivider() {
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setForeground(new Color(230, 233, 238));
        sep.setMaximumSize(new Dimension(2000, 2));
        return sep;
    }

    private static JLabel createMetaLabel(String text, boolean bold) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, 12));
        lbl.setForeground(new Color(40, 50, 65));
        return lbl;
    }

    private static JLabel createBoldMetaLabel(String text, boolean bold) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        lbl.setForeground(Colors.PRIMARY);
        return lbl;
    }

    private static String formatMoney(BigDecimal val) {
        if (val == null) return "₹0.00";
        return "₹" + val.setScale(2, RoundingMode.HALF_UP).toString();
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
        if (pageIndex > 0) return NO_SUCH_PAGE;
        Graphics2D g = (Graphics2D) graphics.create();
        double x = pageFormat.getImageableX();
        double y = pageFormat.getImageableY();
        g.translate(x, y);
        int width = (int) pageFormat.getImageableWidth();
        int line = 18;
        int currentY = line;
        String bName = business != null && business.getName() != null ? business.getName() : "Account Management";
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        g.drawString(bName, 0, currentY); currentY += line;
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
        if (business != null && business.getAddress() != null) { g.drawString(business.getAddress(), 0, currentY); currentY += 13; }
        g.drawString("Invoice: " + (sale.getInvoiceNumber() != null ? sale.getInvoiceNumber() : ""), 0, currentY); currentY += line;
        g.drawString("Date: " + (sale.getSaleDate() == null ? "" : sale.getSaleDate()), 0, currentY); currentY += line;
        g.drawLine(0, currentY, width, currentY); currentY += line;
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 9));
        if (sale.getItems() != null) {
            for (SaleItem item : sale.getItems()) {
                String name = item.getProductName() == null ? "Product #" + item.getProductId() : item.getProductName();
                g.drawString(trim(name, 30), 0, currentY);
                g.drawString(item.getQuantity() + " x " + money(item.getUnitPrice()), width - 150, currentY); currentY += 13;
                g.drawString(money(item.getLineTotal()), width - 55, currentY); currentY += line;
            }
        }
        g.drawLine(0, currentY, width, currentY); currentY += line;
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        g.drawString("TOTAL", 0, currentY); g.drawString(money(sale.getTotal()), width - 65, currentY); currentY += line;
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
        g.drawString("Paid: " + money(sale.getPaidAmount()) + "   Change: " + money(sale.getChangeAmount()), 0, currentY); currentY += line;
        g.drawString("Thank you for your business.", 0, currentY);
        g.dispose();
        return PAGE_EXISTS;
    }

    private static String money(BigDecimal amount) { return "₹" + (amount == null ? "0.00" : amount.setScale(2, RoundingMode.HALF_UP)); }
    private static String trim(String value, int length) { return value.length() <= length ? value : value.substring(0, length - 1) + "…"; }
}
