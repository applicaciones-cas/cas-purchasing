/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.purchasing.services;

import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.LogWrapper;
import org.guanzon.cas.purchasing.controller.PurchaseOrderReturn;

/**
 *
 * @author Arsiela 04-28-2025
 */
public class PurchaseOrderReturnControllers {
    
    private GRiderCAS poGRider;
    private LogWrapper poLogWrapper;
    private PurchaseOrderReturn poPurchaseOrderReturn;
    
    public PurchaseOrderReturnControllers(GRiderCAS applicationDriver, LogWrapper logWrapper){
        poGRider = applicationDriver;
        poLogWrapper = logWrapper;
    }
    
    public PurchaseOrderReturn PurchaseOrderReturn(){
        if (poGRider == null){
            poLogWrapper.severe("PurchaseOrderReturnControllers.PurchaseOrderReturn: Application driver is not set.");
            return null;
        }
        
        if (poPurchaseOrderReturn != null) return poPurchaseOrderReturn;
        
        poPurchaseOrderReturn = new PurchaseOrderReturn();
        poPurchaseOrderReturn.setApplicationDriver(poGRider);
        poPurchaseOrderReturn.setBranchCode(poGRider.getBranchCode());
        poPurchaseOrderReturn.setVerifyEntryNo(true);
        poPurchaseOrderReturn.setWithParent(false);
        poPurchaseOrderReturn.setLogWrapper(poLogWrapper);
        return poPurchaseOrderReturn;        
    }
    
    @Override
    protected void finalize() throws Throwable {
        try {
            poPurchaseOrderReturn = null;
                    
            poLogWrapper = null;
            poGRider = null;
        } finally {
            super.finalize();
        }
    }
}
