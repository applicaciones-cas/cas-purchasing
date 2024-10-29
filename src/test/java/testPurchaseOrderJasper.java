
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.cas.inventory.base.Inventory;
import org.guanzon.cas.parameters.Color;
import org.guanzon.cas.parameters.Model;
import org.guanzon.cas.parameters.Model_Variant;
import org.guanzon.cas.purchasing.controller.PurchaseOrder;
import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author User
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class testPurchaseOrderJasper {
 
    static GRider instance;
    static PurchaseOrder record;

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("sys.default.path.metadata", "D:/GGC_Maven_Systems/config/metadata/");

        instance = MiscUtil.Connect();
        record = new PurchaseOrder(instance, false);
        
        

        if (!loadProperties()) {
            System.err.println("Unable to load config.");
            System.exit(1);
        } else {
            System.out.println("Config file loaded successfully.");
        }

    }

    @Test
    public void testProgramFlow() {
        JSONObject loJSON;
        String lsfsTransNox="M00124000052";
        // This works when the table values are loaded 
        
        loJSON = record.openTransaction(lsfsTransNox);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        
        
       loJSON = record.printRecord();
       if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
    }
    
        private static boolean loadProperties() {
        try {
            Properties po_props = new Properties();
            po_props.load(new FileInputStream("D:\\GGC_Maven_Systems\\config\\cas.properties"));

            System.setProperty("store.branch.code", po_props.getProperty("store.branch.code"));
            System.setProperty("store.inventory.industry", po_props.getProperty("store.inventory.category"));
            
            return true;
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            return false;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }
    

    @AfterClass
    public static void tearDownClass() {
        record = null;
        instance = null;
        
            }

}
