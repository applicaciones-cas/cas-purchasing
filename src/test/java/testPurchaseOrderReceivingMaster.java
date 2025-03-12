
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.cas.purchasing.controller.PurchaseOrderReceiving;
import org.guanzon.cas.purchasing.services.PurchaseOrderReceivingControllers;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Arsiela 03-12-2025
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class testPurchaseOrderReceivingMaster {
    
    static GRider instance;
    static PurchaseOrderReceiving trans;

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("sys.default.path.metadata", "D:/GGC_Maven_Systems/config/metadata/new/");

        instance = MiscUtil.Connect();
        
        trans = new PurchaseOrderReceivingControllers(instance, null).PurchaseOrderReceiving();
    }

//    @Test
//    public void testNewTransaction() {
//        String branchCd = instance.getBranchCode();
//        String industryId = "02";
//        String categoryId = "0002";
//        String remarks = "this is a test RSIE Class.";
//        
//        String stockId = "M00125000001";
//        int quantity = 110;
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
//            trans.Master().setIndustryID(industryId); //direct assignment of value
//            Assert.assertEquals(trans.Master().getIndustryID(), industryId);
//
//            //you can use trans.SearchCategory() when on UI 
////            loJSON = trans.SearchCategory("", false);
////            if (!"success".equals((String) loJSON.get("result"))){
////                System.err.println((String) loJSON.get("message"));
////                Assert.fail();
////            } 
////            trans.Master().setCategoryID(categoryId); //direct assignment of value
////            Assert.assertEquals(trans.Master().getCategoryId(), categoryId);
//
//            trans.Master().setRemarks(remarks);
//            Assert.assertEquals(trans.Master().getRemarks(), remarks);
//
//            trans.Detail(0).setStockID(stockId);
//            trans.Detail(0).setQuantity(quantity);
//
//            trans.AddDetail();
//            trans.Detail(1).setStockID("M00225000111");
//            trans.Detail(1).setQuantity(0);
//
//            trans.AddDetail();
//            trans.Detail(2).setStockID("M00225000222");
//            trans.Detail(2).setQuantity(88);
//            
//            trans.AddDetail();
//            trans.Detail(3).setStockID("M00225000333");
//            trans.Detail(3).setQuantity(50);
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
    
    @Test
    public void testgetPurchaseOrder() {
        String branchCd = instance.getBranchCode();
        String industryId = "02";
        String categoryId = "0002";
        String remarks = "this is a test RSIE Class.";
        
        String stockId = "M00125000001";
        int quantity = 110;
        
        JSONObject loJSON;
        
        loJSON = trans.getApprovedPurchaseOrder();
        if (!"success".equals((String) loJSON.get("result"))) {
            System.err.println((String) loJSON.get("message"));
            Assert.fail();
        }
        
        //retreiving using column index
        for (int lnCtr = 0; lnCtr <= trans.Detail().size() - 1; lnCtr++){
            for (int lnCol = 1; lnCol <= trans.Detail(lnCtr).getColumnCount(); lnCol++){
                System.out.println(trans.Detail(lnCtr).getColumn(lnCol) + " ->> " + trans.Detail(lnCtr).getValue(lnCol));
            }
        }
        
    }
}
