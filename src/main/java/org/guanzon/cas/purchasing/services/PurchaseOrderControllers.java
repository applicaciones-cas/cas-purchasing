package org.guanzon.cas.purchasing.services;

import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.base.LogWrapper;
import org.guanzon.cas.purchasing.controller.PurchaseOrder;
public class PurchaseOrderControllers {
    public PurchaseOrderControllers(GRider applicationDriver, LogWrapper logWrapper){
        poGRider = applicationDriver;
        poLogWrapper = logWrapper;
    }
    
    public PurchaseOrder PurchaseOrder(){
        if (poGRider == null){
            poLogWrapper.severe("PurchaseOrderControllers.PurchaseOrder: Application driver is not set.");
            return null;
        }
        
        if (poPurchaseOrder != null) return poPurchaseOrder;
        
        poPurchaseOrder = new PurchaseOrder();
        poPurchaseOrder.setApplicationDriver(poGRider);
        poPurchaseOrder.setBranchCode(poGRider.getBranchCode());
        poPurchaseOrder.setVerifyEntryNo(true);
        poPurchaseOrder.setWithParent(false);
        poPurchaseOrder.setLogWrapper(poLogWrapper);
        return poPurchaseOrder;        
    }
    
    @Override
    protected void finalize() throws Throwable {
        try {
            poPurchaseOrder = null;
                    
            poLogWrapper = null;
            poGRider = null;
        } finally {
            super.finalize();
        }
    }
    
    private GRider poGRider;
    private LogWrapper poLogWrapper;
    
    private PurchaseOrder poPurchaseOrder;
}
