
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
public class testPurchaseOrderReturn {

    static GRiderCAS poApp;
    static PurchaseOrderControllers poPurchasingController;

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("sys.default.path.metadata", "D:/GGC_Maven_Systems/config/metadata/new/");

        poApp = MiscUtil.Connect();

        poPurchasingController = new PurchaseOrderControllers(poApp, null);
    }

    @Test
    public void testReturnTransaction() {
        JSONObject loJSON;

        try {
            loJSON = poPurchasingController.PurchaseOrder().InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }

            loJSON = poPurchasingController.PurchaseOrder().OpenTransaction("M00125000003");
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }

            //retreiving using column index
            for (int lnCol = 1; lnCol <= poPurchasingController.PurchaseOrder().Master().getColumnCount(); lnCol++) {
                System.out.println(poPurchasingController.PurchaseOrder().Master().getColumn(lnCol) + " ->> "
                        + poPurchasingController.PurchaseOrder().Master().getValue(lnCol));
            }

            //retreiving using column index
            for (int lnCtr = 0; lnCtr <= poPurchasingController.PurchaseOrder().Detail().size() - 1; lnCtr++) {
                for (int lnCol = 1; lnCol <= poPurchasingController.PurchaseOrder().Detail(lnCtr).getColumnCount(); lnCol++) {
                    System.out.println(poPurchasingController.PurchaseOrder().Detail(lnCtr).getColumn(lnCol) + " ->> "
                            + poPurchasingController.PurchaseOrder().Detail(lnCtr).getValue(lnCol));
                }
            }

            loJSON = poPurchasingController.PurchaseOrder().ReturnTransaction("");
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }

            System.out.println((String) loJSON.get("message"));
        } catch (CloneNotSupportedException | ParseException | SQLException | GuanzonException e) {
            System.err.println(MiscUtil.getException(e));
            Assert.fail();
        }

    }
    @AfterClass
    public static void tearDownClass() {
        poPurchasingController = null;
        poApp = null;
    }
}
