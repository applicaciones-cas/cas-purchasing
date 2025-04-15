
import com.lowagie.text.pdf.PdfName;
import java.awt.print.PrinterException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.jasperreports.engine.JRException;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.cas.purchasing.controller.PurchaseOrder;
import org.guanzon.cas.purchasing.services.PurchaseOrderControllers;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class testPurchaseOrderConfirm {

    static GRiderCAS poApp;
    static PurchaseOrderControllers poPurchasingController;

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("sys.default.path.metadata", "D:/GGC_Maven_Systems/config/metadata/new/");

        poApp = MiscUtil.Connect();

        poPurchasingController = new PurchaseOrderControllers(poApp, null);
    }

    @Test
    public void testConfirmTransaction() {
        JSONObject loJSON;

        try {
            loJSON = poPurchasingController.PurchaseOrder().InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }

            loJSON = poPurchasingController.PurchaseOrder().OpenTransaction("M00125000050");
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }

            //retreiving using column index
            System.out.println("MASTER-------------------");
            System.out.println("TRANSACTION NO : " + poPurchasingController.PurchaseOrder().Master().getTransactionNo());
            System.out.println("-------------------------");

            // Display detail records
            for (int lnCtr = 0; lnCtr < poPurchasingController.PurchaseOrder().Detail().size(); lnCtr++) {
                System.out.println("DETAIL-------------------");
                System.out.println("TRANSACTION NO : " + poPurchasingController.PurchaseOrder().Detail(lnCtr).getTransactionNo());
                System.out.println("STOCKID NO : " + poPurchasingController.PurchaseOrder().Detail(lnCtr).getStockID());
                System.out.println("ORDER NO : " + poPurchasingController.PurchaseOrder().Detail(lnCtr).getSouceNo());
                System.out.println("ENTRY NO : " + String.valueOf(poPurchasingController.PurchaseOrder().Detail(lnCtr).getEntryNo()));
                System.out.println("QUANTITY : " + String.valueOf(poPurchasingController.PurchaseOrder().Detail(lnCtr).getQuantity()));
                System.out.println("DETAIL END---------------");

                // Optional: print all detail columns (if needed)
                /*
                for (int lnCol = 1; lnCol <= poPurchasingController.PurchaseOrder().Detail(lnCtr).getColumnCount(); lnCol++) {
                    System.out.println(poPurchasingController.PurchaseOrder().Detail(lnCtr).getColumn(lnCol) + " -> "
                            + poPurchasingController.PurchaseOrder().Detail(lnCtr).getValue(lnCol));
                }
                */
            }

            loJSON = poPurchasingController.PurchaseOrder().ConfirmTransaction("ConfirmTransaction");
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }

            System.out.println((String) loJSON.get("message"));
        } catch (CloneNotSupportedException | ParseException | SQLException | GuanzonException e) {
            System.err.println(MiscUtil.getException(e)) ;
            Assert.fail();
        }
    }
    @AfterClass
    public static void tearDownClass() {
        poPurchasingController = null;
        poApp = null;
    }
}