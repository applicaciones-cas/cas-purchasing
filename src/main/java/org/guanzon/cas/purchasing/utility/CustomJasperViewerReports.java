package org.guanzon.cas.purchasing.utility;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import javafx.application.Platform;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.xml.JRXmlWriter;
import net.sf.jasperreports.export.*;
import net.sf.jasperreports.swing.JRViewer;
import net.sf.jasperreports.swing.JRViewerToolbar;
import net.sf.jasperreports.view.JasperViewer;
import org.guanzon.appdriver.agent.ShowMessageFX;
import ph.com.guanzongroup.cas.cashflow.DisbursementVoucher;

public class CustomJasperViewerReports extends JasperViewer {

    private static final Map<String, CustomJasperViewerReports> OPEN_VIEWERS =
            new HashMap<String, CustomJasperViewerReports>();

    private final String reportTitle;

    public static void show(
            final JasperPrint jasperPrint,
            final String reportTitle) {

        CustomJasperViewerReports viewer =
                OPEN_VIEWERS.get(reportTitle);

        if (viewer != null && viewer.isDisplayable()) {

            viewer.setState(JFrame.NORMAL);
            viewer.toFront();
            viewer.requestFocus();

            return;
        }

        viewer = new CustomJasperViewerReports(
                jasperPrint,
                reportTitle);

        OPEN_VIEWERS.put(reportTitle, viewer);

        viewer.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosed(WindowEvent e) {
                OPEN_VIEWERS.remove(reportTitle);
            }
        });

        viewer.setVisible(true);
    }

    public CustomJasperViewerReports(
            final JasperPrint jasperPrint,
            final String reportTitle) {

        super(jasperPrint, false);

        this.reportTitle = reportTitle;

        customizeToolbar(jasperPrint);
    }

    private void customizeToolbar(final JasperPrint jasperPrint) {

        JRViewer viewer = getJRViewer(getContentPane());

        if (viewer == null) {
            System.out.println("JRViewer not found.");
            return;
        }

        for (Component component : viewer.getComponents()) {

            if (!(component instanceof JRViewerToolbar)) {
                continue;
            }

            JRViewerToolbar toolbar =
                    (JRViewerToolbar) component;

            for (Component c : toolbar.getComponents()) {

                if (c instanceof JButton) {
                    handleButton(
                            (JButton) c,
                            jasperPrint);
                }
            }

            toolbar.revalidate();
            toolbar.repaint();
        }
    }

    private void handleButton(
            JButton button,
            final JasperPrint jasperPrint) {

        String tooltip = button.getToolTipText();

        if ("Save".equals(tooltip)) {

            button.setEnabled(true);
            button.setVisible(true);

            for (ActionListener listener : button.getActionListeners()) {
                button.removeActionListener(listener);
            }

            button.addActionListener(e -> saveReport(jasperPrint));
            return;
        }

        if ("Print".equals(tooltip)) {

            for (ActionListener listener :
                    button.getActionListeners()) {

                button.removeActionListener(listener);
            }

            button.addActionListener(
                    e -> printReport(jasperPrint));
        }
    }

    private void printReport(
            final JasperPrint jasperPrint) {

        try {

            boolean printed =
                    JasperPrintManager.printReport(
                            jasperPrint,
                            true);

            if (!printed) {

                Platform.runLater(() ->
                        ShowMessageFX.Warning(
                                "Printing was canceled by the user.",
                                reportTitle,
                                null));

                toFront();

                return;
            }

            Platform.runLater(() ->
                    ShowMessageFX.Information(
                            "Report printed successfully.",
                            reportTitle,
                            null));

        } catch (JRException ex) {

            Platform.runLater(() ->
                    ShowMessageFX.Warning(
                            "Print failed: "
                                    + ex.getMessage(),
                            reportTitle,
                            null));

            toFront();
        }
    }

    private JRViewer getJRViewer(
            Container container) {

        for (Component component :
                container.getComponents()) {

            if (component instanceof JRViewer) {
                return (JRViewer) component;
            }

            if (component instanceof Container) {

                JRViewer viewer =
                        getJRViewer(
                                (Container) component);

                if (viewer != null) {
                    return viewer;
                }
            }
        }

        return null;
    }

    private void saveReport(final JasperPrint jasperPrint) {

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Report");

        chooser.addChoosableFileFilter(
                new FileNameExtensionFilter("PDF (*.pdf)", "pdf"));

        chooser.addChoosableFileFilter(
                new FileNameExtensionFilter("Excel Workbook (*.xlsx)", "xlsx"));

        chooser.addChoosableFileFilter(
                new FileNameExtensionFilter("Word Document (*.docx)", "docx"));

        chooser.addChoosableFileFilter(
                new FileNameExtensionFilter("CSV (*.csv)", "csv"));

        chooser.addChoosableFileFilter(
                new FileNameExtensionFilter("HTML (*.html)", "html"));

        chooser.addChoosableFileFilter(
                new FileNameExtensionFilter("JR XML (*.jrpxml)", "jrpxml"));

        chooser.setAcceptAllFileFilterUsed(false);

        int result = chooser.showSaveDialog(this);

        if (result != JFileChooser.APPROVE_OPTION) {

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    ShowMessageFX.Warning(
                            "Saving was canceled by the user.",
                            reportTitle,
                            null
                    );
                }
            });

            return;
        }

        try {

            FileNameExtensionFilter filter =
                    (FileNameExtensionFilter) chooser.getFileFilter();

            String extension = filter.getExtensions()[0];

            File file = chooser.getSelectedFile();
            String fileName = file.getAbsolutePath();

            if (!fileName.toLowerCase().endsWith("." + extension)) {
                fileName += "." + extension;
            }

            switch (extension.toLowerCase()) {

                case "pdf":

                    JasperExportManager.exportReportToPdfFile(
                            jasperPrint,
                            fileName
                    );

                    break;

                case "xlsx": {

                    JRXlsxExporter exporter = new JRXlsxExporter();

                    exporter.setExporterInput(
                            new SimpleExporterInput(jasperPrint));

                    exporter.setExporterOutput(
                            new SimpleOutputStreamExporterOutput(fileName));

                    SimpleXlsxReportConfiguration config =
                            new SimpleXlsxReportConfiguration();

                    config.setOnePagePerSheet(false);
                    config.setDetectCellType(true);
                    config.setCollapseRowSpan(false);

                    exporter.setConfiguration(config);
                    exporter.exportReport();

                    break;
                }

                case "docx": {

                    JRDocxExporter exporter = new JRDocxExporter();

                    exporter.setExporterInput(
                            new SimpleExporterInput(jasperPrint));

                    exporter.setExporterOutput(
                            new SimpleOutputStreamExporterOutput(fileName));

                    exporter.exportReport();

                    break;
                }

                case "csv": {

                    JRCsvExporter exporter = new JRCsvExporter();

                    exporter.setExporterInput(
                            new SimpleExporterInput(jasperPrint));

                    exporter.setExporterOutput(
                            new SimpleWriterExporterOutput(fileName));

                    exporter.exportReport();

                    break;
                }

                case "html": {

                    HtmlExporter exporter = new HtmlExporter();

                    exporter.setExporterInput(
                            new SimpleExporterInput(jasperPrint));

                    exporter.setExporterOutput(
                            new SimpleHtmlExporterOutput(fileName));

                    exporter.exportReport();

                    break;
                }

                case "jrpxml":

                    JRXmlWriter.writeReport(
                            (JRReport) jasperPrint,
                            fileName,
                            "UTF-8"
                    );

                    break;

                default:

                    throw new IllegalArgumentException(
                            "Unsupported file format: " + extension
                    );
            }

            final String savedFile = fileName;

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    ShowMessageFX.Information(
                            "Report saved successfully.\n\n" + savedFile,
                            reportTitle,
                            null
                    );
                }
            });

        } catch (final Exception ex) {

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    ShowMessageFX.Warning(
                            "Save failed:\n" + ex.getMessage(),
                            reportTitle,
                            null
                    );
                }
            });

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    CustomJasperViewerReports .this.setVisible(true);
                    CustomJasperViewerReports .this.toFront();
                    CustomJasperViewerReports .this.requestFocus();
                }
            });
        }
    }
}