package org.guanzon.cas.purchasing.services;

import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.LogWrapper;
import org.guanzon.cas.purchasing.controller.POCancellation;

public class POController {

    private GRiderCAS poGRider;
    private LogWrapper poLogWrapper;

    private POCancellation poPOCancellation;

    public POController(GRiderCAS applicationDriver, LogWrapper logWrapper) {
        poGRider = applicationDriver;
        poLogWrapper = logWrapper;
    }

    public POCancellation POCancellation() {
        if (poGRider == null) {
            poLogWrapper.severe("POController.POCancellation: Application driver is not set.");
            return null;
        }

        if (poPOCancellation != null) {
            return poPOCancellation;
        }

        poPOCancellation = new POCancellation();
        poPOCancellation.setApplicationDriver(poGRider);
        poPOCancellation.setBranchCode(poGRider.getBranchCode());
        poPOCancellation.setVerifyEntryNo(true);
        poPOCancellation.setWithParent(false);
        poPOCancellation.setLogWrapper(poLogWrapper);
        return poPOCancellation;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            poPOCancellation = null;

            poLogWrapper = null;
            poGRider = null;
        } finally {
            super.finalize();
        }
    }

}
