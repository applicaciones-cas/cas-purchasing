

import java.math.BigDecimal;
import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.TransactionStatus;
import org.guanzon.cas.purchasing.controller.PurchaseOrder;
import org.guanzon.cas.purchasing.testing.*;
import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

//public class testPurchaseOrderPosting {
    //creare
    //save
    //update
    //save
    
    //closetransaction(printing)
//}


//
//public class testPurchaseOrderNew {
//    //1. Matched yung values na pinass natin
//    //2. Check kung yung nasave sa database after saving is matched sa mga pinass
//    //3. Delete yung mga sinave
//    
//    
//    
//    
//}

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class testPurchaseOrderPosting {
 
    static GRider instance;
    static PurchaseOrder record;

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("sys.default.path.metadata", "D:/GGC_Maven_Systems/config/metadata/");

        instance = MiscUtil.Connect();
        record = new PurchaseOrder(instance, false);
    }

    @Test
    public void testProgramFlow() {
        JSONObject loJSON;

        //setting testing
        loJSON = record.postTransaction("M00124000001");
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        
        
        //set master information
        loJSON = record.getMasterModel().setBranchCode("M001");
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setTransDate(SQLUtil.toDate("2024-04-25",SQLUtil.FORMAT_SHORT_DATE));
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setCompanyID("M001");
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setDestination("GK01");
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setSupplier("M00124000001");
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setAddressID("M00124000001");
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setContactID("M00124000001");
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setReferNo("M00124000001");
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setTermCode("M001241");
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setTransactionTotal(BigDecimal.valueOf(1999.99));
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setVatRate(BigDecimal.valueOf(9.99));
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setVatAmount(BigDecimal.valueOf(199.80));
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setVATAdded("1");
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setTaxWithHolding(BigDecimal.valueOf(0.00));
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setDiscount(BigDecimal.valueOf(0.00));
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setAddDiscount(BigDecimal.valueOf(0.00));
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setAmountPaid(BigDecimal.valueOf(1999.99));
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setNetTotal(BigDecimal.valueOf(1800.19));
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setRemarks("This is a Test for Purchase Order");
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setSourceCode("CaPO");
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setSourceNo("M00124000001");
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setEmailSentStatus("0");
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setEmailSentNo(1);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setEntryNo(1);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setCategoryCode("0001");
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setPOrderType("0");
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setTransactionStatus(TransactionStatus.STATE_OPEN);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setPreparedBy("M00120001698");
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setPreparedDate(instance.getServerDate());
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setApprovedBy(null);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        //dateapproved
        //approvcode
        //posted == postedby
        //posteddate
        //
        
        
//        used only in approval method
//        loJSON = record.getMasterModel().setApprovedate(instance.getServerDate());
//        if ("error".equals((String) loJSON.get("result"))) {
//            Assert.fail((String) loJSON.get("message"));
//        }
        loJSON = record.getMasterModel().setPostedBy(null);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
//        used only in Posting method
        loJSON = record.getMasterModel().setPostedDate(instance.getServerDate());
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }

        loJSON = record.getMasterModel().setModifiedBy(instance.getUserID());
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getMasterModel().setModifiedDate(instance.getServerDate());
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        
        
        //set detail information
        
        
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setStockID("M00124000001");
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setDescription("This is Detail 02");
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setQtyOnHand(0);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setROQQuantity(1);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setOrderQuantity(1);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setQuantity(1);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setUnitPrice(BigDecimal.valueOf(1800.19));
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setReceiveNo(1);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setCancelledNo(0);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setModifiedDate(instance.getServerDate());
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        
        //set detail 2 information
        record.AddModelDetail();
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setStockID("M00124000002");
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setDescription("This is Detail 03 entry no2");
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setQtyOnHand(0);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setROQQuantity(1);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setOrderQuantity(1);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setQuantity(1);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setUnitPrice(BigDecimal.valueOf(1800.19));
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setReceiveNo(1);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setCancelledNo(0);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setModifiedDate(instance.getServerDate());
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        
        
        
        loJSON = record.saveTransaction();
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
    }

    @AfterClass
    public static void tearDownClass() {
        record = null;
        instance = null;
    }
}
