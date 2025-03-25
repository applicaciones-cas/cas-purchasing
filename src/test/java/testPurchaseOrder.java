
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
public class testPurchaseOrder {

    static GRiderCAS poApp;
    static PurchaseOrderControllers poPurchasingController;

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("sys.default.path.metadata", "D:/GGC_Maven_Systems/config/metadata/new/");

        poApp = MiscUtil.Connect();

        poPurchasingController = new PurchaseOrderControllers(poApp, null);
    }

//    @Test
//    public void testGetApprovedStockRequests() {
//        try {
//            JSONObject response = (JSONObject) poPurchasingController.PurchaseOrder().getApprovedStockRequests();  // Direct JSONObject response
//
//            // Ensure the response is not null
//            Assert.assertNotNull("Response should not be null", response);
//
//            // Extract the list of approved stock requests from the JSON response
//            if (!"success".equals(response.get("result"))) {
//                System.err.println("Failed to fetch approved stock requests: " + response.get("message"));
//                Assert.fail();
//            }
//
//            // Assuming "data" holds the list of approved stock requests
//            JSONArray approvedRequests = (JSONArray) response.get("data");
//
//            // Validate that the list is not null or empty
//            Assert.assertNotNull("Approved stock requests should not be null", approvedRequests);
//            Assert.assertFalse("Approved stock requests should not be empty", approvedRequests.isEmpty());
//
//            System.out.println("Total Approved Stock Requests: " + approvedRequests.size());
//
//            // Iterate through the list and validate each request
//            for (Object obj : approvedRequests) {
//                JSONObject request = (JSONObject) obj;
//                Assert.assertEquals("Status should be 'approved'", null, request.get("sApproved"));
//                System.out.println("Approved Stock Request: " + request.toJSONString());
//            }
//
//        } catch (Exception e) {
//            System.err.println("Exception occurred while fetching approved stock requests: " + e.getMessage());
//            e.printStackTrace();
//            Assert.fail("Exception thrown during test execution.");
//        }
//    }
    @Test
    public void testGetApprovedRequest() {
        JSONObject loJSON;
        try {
            loJSON = poPurchasingController.PurchaseOrder().InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }

            try {
                loJSON = poPurchasingController.PurchaseOrder().getApprovedStockRequests();
            } catch (SQLException | GuanzonException ex) {
                Logger.getLogger(testPurchaseOrder.class.getName()).log(Level.SEVERE, null, ex);
            }
            if ("success".equals((String) loJSON.get("result"))) {
                System.out.println("RESULT" + (String) loJSON.get("message"));
                for (int lnCntr = 0; lnCntr <= poPurchasingController.PurchaseOrder().getInvStockRequestCount() - 1; lnCntr++) {
                    System.out.println("Transaction no:" + poPurchasingController.PurchaseOrder().InvStockRequestMaster(lnCntr).getTransactionNo());
                    System.out.println("Entry no:" + poPurchasingController.PurchaseOrder().InvStockRequestMaster(lnCntr).getEntryNo());
                }
            }
        } catch (ExceptionInInitializerError e) {
            System.err.println(MiscUtil.getException(e));
            Assert.fail();
        }

    }

    @Test
    public void testPOMaster() {
        JSONObject loJSON;
        try {
            loJSON = poPurchasingController.PurchaseOrder().InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }

            loJSON = poPurchasingController.PurchaseOrder().getPurchaseOrder();

            if ("success".equals((String) loJSON.get("result"))) {
                System.out.println("RESULT" + (String) loJSON.get("message"));
                for (int lnCntr = 0; lnCntr <= poPurchasingController.PurchaseOrder().getPOMasterCount() - 1; lnCntr++) {
                    System.out.println("poPurchasingController no:" + poPurchasingController.PurchaseOrder().POMaster(lnCntr).getTransactionNo());
                    System.out.println("poPurchasingController entry no:" + poPurchasingController.PurchaseOrder().POMaster(lnCntr).getEntryNo());
                    System.out.println("poPurchasingController status:" + poPurchasingController.PurchaseOrder().POMaster(lnCntr).getTransactionStatus());
                }
            }
        } catch (ExceptionInInitializerError | SQLException | GuanzonException e) {
            System.err.println(MiscUtil.getException(e));
            Assert.fail();
        }

    }

    @Test
    public void testNewTransaction() {
        String branchCd = poApp.getBranchCode();
        String industryId = "02";
        String companyId = "0002";
        String supplierId = "0002";
        String destinationId = poApp.getBranchCode();
        String remarks = "this is a test.";
        String stockId = "M00125000001";
        String description = "TMX Supremo Standard 2025";
        double nOldPrice = 0.00;
        double nUnitPrce = 0.00;
        int nQtyOnHnd = 110;
        int nRecOrder = 110;
        int quantity = 110;
        int nReceived = 0;
        int nCancelld = 0;
        String classify = "A";
        int recOrder = 110;
        int onHand = 10;

        JSONObject loJSON;

        try {
            loJSON = poPurchasingController.PurchaseOrder().InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }

            loJSON = poPurchasingController.PurchaseOrder().NewTransaction();
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }

            poPurchasingController.PurchaseOrder().Master().setIndustryID(industryId); //direct assignment of value
            Assert.assertEquals(poPurchasingController.PurchaseOrder().Master().getIndustryID(), industryId);

            poPurchasingController.PurchaseOrder().Master().setCompanyID(companyId); //direct assignment of value
            Assert.assertEquals(poPurchasingController.PurchaseOrder().Master().getCompanyID(), companyId);

            poPurchasingController.PurchaseOrder().Master().setSupplierID(supplierId); //direct assignment of value
            Assert.assertEquals(poPurchasingController.PurchaseOrder().Master().getCompanyID(), supplierId);

            poPurchasingController.PurchaseOrder().Master().setBranchCode(branchCd); //direct assignment of value
            Assert.assertEquals(poPurchasingController.PurchaseOrder().Master().getBranchCode(), branchCd);

            poPurchasingController.PurchaseOrder().Master().setDestinationID(destinationId); //direct assignment of value
            Assert.assertEquals(poPurchasingController.PurchaseOrder().Master().getDestinationID(), destinationId);

            poPurchasingController.PurchaseOrder().Master().setRemarks(remarks);
            Assert.assertEquals(poPurchasingController.PurchaseOrder().Master().getRemarks(), remarks);

            poPurchasingController.PurchaseOrder().Detail(0).setEntryNo(1);
            poPurchasingController.PurchaseOrder().Detail(0).setStockID(stockId);
            poPurchasingController.PurchaseOrder().Detail(0).setDescription(description);
            poPurchasingController.PurchaseOrder().Detail(0).setOldPrice(nOldPrice);
            poPurchasingController.PurchaseOrder().Detail(0).setUnitPrice(nUnitPrce);
            poPurchasingController.PurchaseOrder().Detail(0).setQuantityOnHand(nQtyOnHnd);
            poPurchasingController.PurchaseOrder().Detail(0).setRecordOrder(nRecOrder);
            poPurchasingController.PurchaseOrder().Detail(0).setQuantity(quantity);
            poPurchasingController.PurchaseOrder().Detail(0).setReceivedQuantity(nReceived);
            poPurchasingController.PurchaseOrder().Detail(0).setCancelledQuantity(nCancelld);
            poPurchasingController.PurchaseOrder().AddDetail();

            poPurchasingController.PurchaseOrder().Detail(1).setEntryNo(2);
            poPurchasingController.PurchaseOrder().Detail(1).setStockID("M00225000111");
            poPurchasingController.PurchaseOrder().Detail(1).setDescription(description);
            poPurchasingController.PurchaseOrder().Detail(1).setOldPrice(nOldPrice);
            poPurchasingController.PurchaseOrder().Detail(1).setUnitPrice(nUnitPrce);
            poPurchasingController.PurchaseOrder().Detail(1).setQuantityOnHand(nQtyOnHnd);
            poPurchasingController.PurchaseOrder().Detail(1).setRecordOrder(nRecOrder);
            poPurchasingController.PurchaseOrder().Detail(1).setQuantity(quantity);
            poPurchasingController.PurchaseOrder().Detail(1).setReceivedQuantity(nReceived);
            poPurchasingController.PurchaseOrder().Detail(1).setCancelledQuantity(nCancelld);
            poPurchasingController.PurchaseOrder().AddDetail();

            poPurchasingController.PurchaseOrder().Detail(2).setEntryNo(3);
            poPurchasingController.PurchaseOrder().Detail(2).setStockID("M00225000222");
            poPurchasingController.PurchaseOrder().Detail(2).setDescription(description);
            poPurchasingController.PurchaseOrder().Detail(2).setOldPrice(nOldPrice);
            poPurchasingController.PurchaseOrder().Detail(2).setUnitPrice(nUnitPrce);
            poPurchasingController.PurchaseOrder().Detail(2).setQuantityOnHand(nQtyOnHnd);
            poPurchasingController.PurchaseOrder().Detail(2).setRecordOrder(nRecOrder);
            poPurchasingController.PurchaseOrder().Detail(2).setQuantity(quantity);
            poPurchasingController.PurchaseOrder().Detail(2).setReceivedQuantity(nReceived);
            poPurchasingController.PurchaseOrder().Detail(2).setCancelledQuantity(nCancelld);
            poPurchasingController.PurchaseOrder().AddDetail();

            loJSON = poPurchasingController.PurchaseOrder().SaveTransaction();

            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }
        } catch (CloneNotSupportedException | SQLException | ExceptionInInitializerError | GuanzonException e) {
            System.err.println(MiscUtil.getException(e));
            Assert.fail();
        }
    }

    @Test
    public void testPrintTransaction() {
        JSONObject loJSON = new JSONObject();
        try {
            loJSON = poPurchasingController.PurchaseOrder().InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }

            loJSON = poPurchasingController.PurchaseOrder().OpenTransaction("M00125000007");

            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }
            loJSON = poPurchasingController.PurchaseOrder().printTransaction();

        } catch (CloneNotSupportedException | SQLException | GuanzonException e) {
            System.err.println(MiscUtil.getException(e));
            Assert.fail();
        }
    }

    @Test
    public void testOpenTransaction() {
        JSONObject loJSON;
        try {
            loJSON = poPurchasingController.PurchaseOrder().InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }

            loJSON = poPurchasingController.PurchaseOrder().OpenTransaction("M00125000002");

            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }

            //retreiving using column index
            for (int lnCol = 1; lnCol <= poPurchasingController.PurchaseOrder().Master().getColumnCount(); lnCol++) {
                System.out.println(poPurchasingController.PurchaseOrder().Master().getColumn(lnCol) + " ->> " + poPurchasingController.PurchaseOrder().Master().getValue(lnCol));
            }
            //retreiving using field descriptions
            System.out.println(poPurchasingController.PurchaseOrder().Master().Branch().getBranchName());
            System.out.println(poPurchasingController.PurchaseOrder().Master().Category().getDescription());

            //retreiving using column index
            for (int lnCtr = 0; lnCtr <= poPurchasingController.PurchaseOrder().Detail().size() - 1; lnCtr++) {
                for (int lnCol = 1; lnCol <= poPurchasingController.PurchaseOrder().Detail(lnCtr).getColumnCount(); lnCol++) {
                    System.out.println(poPurchasingController.PurchaseOrder().Detail(lnCtr).getColumn(lnCol) + " ->> " + poPurchasingController.PurchaseOrder().Detail(lnCtr).getValue(lnCol));
                }
            }
        } catch (CloneNotSupportedException | SQLException | GuanzonException e) {
            System.err.println(MiscUtil.getException(e));
            Assert.fail();
        }

    }

    @Test
    public void testUpdateTransaction() throws GuanzonException {
        JSONObject loJSON;

        try {
            loJSON = (JSONObject) poPurchasingController.PurchaseOrder().InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }

            loJSON = (JSONObject) poPurchasingController.PurchaseOrder().OpenTransaction("A00125000001");
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }

            loJSON = (JSONObject) poPurchasingController.PurchaseOrder().UpdateTransaction();
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }

            poPurchasingController.PurchaseOrder().Detail(0).setQuantityOnHand(12);
            poPurchasingController.PurchaseOrder().Detail(0).setModifiedDate(poApp.getServerDate());
            poPurchasingController.PurchaseOrder().Detail(1).setQuantityOnHand(22);
            poPurchasingController.PurchaseOrder().Detail(1).setModifiedDate(poApp.getServerDate());

            loJSON = poPurchasingController.PurchaseOrder().SaveTransaction();
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }
        } catch (CloneNotSupportedException | SQLException e) {
            System.err.println(MiscUtil.getException(e));
            Assert.fail();
        }

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

            loJSON = poPurchasingController.PurchaseOrder().OpenTransaction("M00125000003");
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }

            //retreiving using column index
            for (int lnCol = 1; lnCol <= poPurchasingController.PurchaseOrder().Master().getColumnCount(); lnCol++) {
                System.out.println(poPurchasingController.PurchaseOrder().Master().getColumn(lnCol) + " ->> " + poPurchasingController.PurchaseOrder().Master().getValue(lnCol));
            }
            //retreiving using field descriptions
            System.out.println(poPurchasingController.PurchaseOrder().Master().Branch().getBranchName());
            System.out.println(poPurchasingController.PurchaseOrder().Master().Category().getDescription());

            //retreiving using column index
            for (int lnCtr = 0; lnCtr <= poPurchasingController.PurchaseOrder().Detail().size() - 1; lnCtr++) {
                for (int lnCol = 1; lnCol <= poPurchasingController.PurchaseOrder().Detail(lnCtr).getColumnCount(); lnCol++) {
                    System.out.println(poPurchasingController.PurchaseOrder().Detail(lnCtr).getColumn(lnCol) + " ->> " + poPurchasingController.PurchaseOrder().Detail(lnCtr).getValue(lnCol));
                }
            }

            loJSON = poPurchasingController.PurchaseOrder().ConfirmTransaction("");

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
