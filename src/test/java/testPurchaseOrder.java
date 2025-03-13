
import java.sql.SQLException;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.cas.purchasing.controller.PurchaseOrder;
import org.guanzon.cas.purchasing.services.PurchaseOrderControllers;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class testPurchaseOrder {

    static GRiderCAS instance;
    static PurchaseOrder trans;

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("sys.default.path.metadata", "D:/GGC_Maven_Systems/config/metadata/new/");

        instance = MiscUtil.Connect();

        trans = new PurchaseOrderControllers(instance, null).PurchaseOrder();
    }

    @Test
    public void testGetApprovedStockRequests() {
        try {
            JSONObject response = (JSONObject) trans.getApprovedStockRequests();  // Direct JSONObject response

            // Ensure the response is not null
            Assert.assertNotNull("Response should not be null", response);

            // Extract the list of approved stock requests from the JSON response
            if (!"success".equals(response.get("result"))) {
                System.err.println("Failed to fetch approved stock requests: " + response.get("message"));
                Assert.fail();
            }

            // Assuming "data" holds the list of approved stock requests
            JSONArray approvedRequests = (JSONArray) response.get("data");

            // Validate that the list is not null or empty
            Assert.assertNotNull("Approved stock requests should not be null", approvedRequests);
            Assert.assertFalse("Approved stock requests should not be empty", approvedRequests.isEmpty());

            System.out.println("Total Approved Stock Requests: " + approvedRequests.size());

            // Iterate through the list and validate each request
            for (Object obj : approvedRequests) {
                JSONObject request = (JSONObject) obj;
                Assert.assertEquals("Status should be 'approved'", null, request.get("sApproved"));
                System.out.println("Approved Stock Request: " + request.toJSONString());
            } 

        } catch (Exception e) {
            System.err.println("Exception occurred while fetching approved stock requests: " + e.getMessage());
            e.printStackTrace();
            Assert.fail("Exception thrown during test execution.");
        }
    }

//    @Test
//    public void testNewTransaction() {
//        String branchCd = instance.getBranchCode();
//        String industryId = "02";
//        String categoryId = "0002";
//        String remarks = "this is a test.";
//        
//        String stockId = "M00125000001";
//        int quantity = 110;
//        String classify = "A";
//        int recOrder = 110;
//        int onHand = 10;
//        
//        JSONObject loJSON;
//        
//        try {
//            loJSON = trans.InitTransaction();
//            if (!"success".equals((String) loJSON.get("result"))){
//                System.err.println((String) loJSON.get("message"));
//                Assert.fail();
//            } 
//            
//            loJSON = trans.NewTransaction();
//            if (!"success".equals((String) loJSON.get("result"))){
//                System.err.println((String) loJSON.get("message"));
//                Assert.fail();
//            } 
//            
//            //you can use trans.SearchBranch() when on UI 
////            loJSON = trans.SearchBranch("", false);
////            if (!"success".equals((String) loJSON.get("result"))){
////                System.err.println((String) loJSON.get("message"));
////                Assert.fail();
////            } 
//            trans.Master().setBranchCode(branchCd); //direct assignment of value
//            Assert.assertEquals(trans.Master().getBranchCode(), branchCd);
//
//            //you can use trans.SearchIndustry() when on UI 
////            loJSON = trans.SearchIndustry("", false);
////            if (!"success".equals((String) loJSON.get("result"))){
////                System.err.println((String) loJSON.get("message"));
////                Assert.fail();
////            } 
//            trans.Master().setIndustryId(industryId); //direct assignment of value
//            Assert.assertEquals(trans.Master().getIndustryId(), industryId);
//
//            //you can use trans.SearchCategory() when on UI 
////            loJSON = trans.SearchCategory("", false);
////            if (!"success".equals((String) loJSON.get("result"))){
////                System.err.println((String) loJSON.get("message"));
////                Assert.fail();
////            } 
//            trans.Master().setCategoryId(categoryId); //direct assignment of value
//            Assert.assertEquals(trans.Master().getCategoryId(), categoryId);
//
//            trans.Master().setRemarks(remarks);
//            Assert.assertEquals(trans.Master().getRemarks(), remarks);
//
//            trans.Detail(0).setStockId(stockId);
//            trans.Detail(0).setQuantity(quantity);
//            trans.Detail(0).setClassification(classify);
//            trans.Detail(0).setRecommendedOrder(recOrder);
//            trans.Detail(0).setQuantityOnHand(onHand);
//
//            trans.AddDetail();
//            trans.Detail(1).setStockId("M00225000111");
//            trans.Detail(1).setQuantity(0);
//            trans.Detail(1).setClassification(classify);
//            trans.Detail(1).setRecommendedOrder(recOrder);
//            trans.Detail(1).setQuantityOnHand(onHand);
//
//            trans.AddDetail();
//            trans.Detail(2).setStockId("M00225000222");
//            trans.Detail(2).setQuantity(10);
//            trans.Detail(2).setClassification(classify);
//            trans.Detail(2).setRecommendedOrder(recOrder);
//            trans.Detail(2).setQuantityOnHand(onHand);
//
//            trans.AddDetail();
//            trans.Detail(3).setStockId("M00225000333");
//            trans.Detail(3).setQuantity(50);
//            trans.Detail(3).setClassification(classify);
//            trans.Detail(3).setRecommendedOrder(recOrder);
//            trans.Detail(3).setQuantityOnHand(onHand);
//
//            trans.AddDetail();
//
//            loJSON = trans.SaveTransaction();
//            if (!"success".equals((String) loJSON.get("result"))) {
//                System.err.println((String) loJSON.get("message"));
//                Assert.fail();
//            }
//        } catch (CloneNotSupportedException | SQLException | ExceptionInInitializerError e) {
//            System.err.println(MiscUtil.getException(e));
//            Assert.fail();
//        }
//    }   
//    @Test
//    public void testOpenTransaction() {
//        JSONObject loJSON;
//        
//        try {
//            loJSON = trans.InitTransaction();
//            if (!"success".equals((String) loJSON.get("result"))){
//                System.err.println((String) loJSON.get("message"));
//                Assert.fail();
//            } 
//
//            loJSON = trans.OpenTransaction("M00125000002");
//            if (!"success".equals((String) loJSON.get("result"))){
//                System.err.println((String) loJSON.get("message"));
//                Assert.fail();
//            } 
//
//            //retreiving using column index
//            for (int lnCol = 1; lnCol <= trans.Master().getColumnCount(); lnCol++){
//                System.out.println(trans.Master().getColumn(lnCol) + " ->> " + trans.Master().getValue(lnCol));
//            }
//            //retreiving using field descriptions
//            System.out.println(trans.Master().Branch().getBranchName());
//            System.out.println(trans.Master().Category().getDescription());
//
//            //retreiving using column index
//            for (int lnCtr = 0; lnCtr <= trans.Detail().size() - 1; lnCtr++){
//                for (int lnCol = 1; lnCol <= trans.Detail(lnCtr).getColumnCount(); lnCol++){
//                    System.out.println(trans.Detail(lnCtr).getColumn(lnCol) + " ->> " + trans.Detail(lnCtr).getValue(lnCol));
//                }
//            }
//        } catch (CloneNotSupportedException e) {
//            System.err.println(MiscUtil.getException(e));
//            Assert.fail();
//        }
//        
//        
//    }   
//    
//    @Test
    public void testUpdateTransaction() {
        JSONObject loJSON;

        try {
            loJSON = (JSONObject) trans.InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }

            loJSON = (JSONObject) trans.OpenTransaction("A00125000001");
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }

            loJSON = (JSONObject) trans.UpdateTransaction();
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }

            trans.Detail(0).setQuantityOnHand(12);
            trans.Detail(1).setQuantityOnHand(22);
//            trans.AddDetail();

            loJSON = trans.SaveTransaction();
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }
        } catch (CloneNotSupportedException | SQLException e) {
            System.err.println(MiscUtil.getException(e));
            Assert.fail();
        }

    }
//    
//    @Test
//    public void testConfirmTransaction() {
//        JSONObject loJSON;
//        
//        try {
//            loJSON = trans.InitTransaction();
//            if (!"success".equals((String) loJSON.get("result"))){
//                System.err.println((String) loJSON.get("message"));
//                Assert.fail();
//            } 
//
//            loJSON = trans.OpenTransaction("M00125000003");
//            if (!"success".equals((String) loJSON.get("result"))){
//                System.err.println((String) loJSON.get("message"));
//                Assert.fail();
//            } 
//
//            //retreiving using column index
//            for (int lnCol = 1; lnCol <= trans.Master().getColumnCount(); lnCol++){
//                System.out.println(trans.Master().getColumn(lnCol) + " ->> " + trans.Master().getValue(lnCol));
//            }
//            //retreiving using field descriptions
//            System.out.println(trans.Master().Branch().getBranchName());
//            System.out.println(trans.Master().Category().getDescription());
//
//            //retreiving using column index
//            for (int lnCtr = 0; lnCtr <= trans.Detail().size() - 1; lnCtr++){
//                for (int lnCol = 1; lnCol <= trans.Detail(lnCtr).getColumnCount(); lnCol++){
//                    System.out.println(trans.Detail(lnCtr).getColumn(lnCol) + " ->> " + trans.Detail(lnCtr).getValue(lnCol));
//                }
//            }
//            
//            loJSON = trans.ConfirmTransaction("");
//            if (!"success".equals((String) loJSON.get("result"))){
//                System.err.println((String) loJSON.get("message"));
//                Assert.fail();
//            } 
//            
//            System.out.println((String) loJSON.get("message"));
//        } catch (CloneNotSupportedException |ParseException e) {
//            System.err.println(MiscUtil.getException(e));
//            Assert.fail();
//        }
//        
//        
//    }   

    @AfterClass
    public static void tearDownClass() {
        trans = null;
        instance = null;
    }
}
