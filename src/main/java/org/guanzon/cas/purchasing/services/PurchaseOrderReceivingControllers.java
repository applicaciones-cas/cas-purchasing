/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.purchasing.services;

import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.LogWrapper;
import org.guanzon.cas.purchasing.controller.PurchaseOrderReceiving;

/**
 *
 * @author Arsiela 03-12-2025
 */
public class PurchaseOrderReceivingControllers {
    
    private GRiderCAS poGRider;
    private LogWrapper poLogWrapper;
    private PurchaseOrderReceiving poPurchaseOrderReceiving;
    
    public PurchaseOrderReceivingControllers(GRiderCAS applicationDriver, LogWrapper logWrapper){
        poGRider = applicationDriver;
        poLogWrapper = logWrapper;
    }
    
    public PurchaseOrderReceiving PurchaseOrderReceiving(){
        if (poGRider == null){
            poLogWrapper.severe("PurchaseOrderReceivingControllers.PurchaseOrderReceiving: Application driver is not set.");
            return null;
        }
        
        if (poPurchaseOrderReceiving != null) return poPurchaseOrderReceiving;
        
        poPurchaseOrderReceiving = new PurchaseOrderReceiving();
        poPurchaseOrderReceiving.setApplicationDriver(poGRider);
        poPurchaseOrderReceiving.setBranchCode(poGRider.getBranchCode());
        poPurchaseOrderReceiving.setVerifyEntryNo(true);
        poPurchaseOrderReceiving.setWithParent(false);
        poPurchaseOrderReceiving.setLogWrapper(poLogWrapper);
        return poPurchaseOrderReceiving;        
    }
    
    @Override
    protected void finalize() throws Throwable {
        try {
            poPurchaseOrderReceiving = null;
                    
            poLogWrapper = null;
            poGRider = null;
        } finally {
            super.finalize();
        }
    }
}
