/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.nwtable;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestTableMTUngroupedIntoTableWriteMultiWriterAgg extends TestCase
{
    private static final Logger log = LoggerFactory.getLogger(TestTableMTUngroupedIntoTableWriteMultiWriterAgg.class);

    private EPServiceProvider epService;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType(SupportBean.class);
        config.addEventType(SupportBean_S0.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
    }

    /**
     * For a given number of seconds:
     * Configurable number of into-writers update a shared aggregation.
     * At the end of the test we read and assert.
     */
    public void testMT() throws Exception
    {
        tryMT(3, 10000);
    }

    private void tryMT(int numThreads, int numEvents) throws Exception
    {
        String eplCreateVariable = "create table varagg (theEvents window(*) @type(SupportBean))";
        epService.getEPAdministrator().createEPL(eplCreateVariable);

        Thread[] threads = new Thread[numThreads];
        WriteRunnable[] runnables = new WriteRunnable[numThreads];
        for (int i = 0; i < threads.length; i++) {
            runnables[i] = new WriteRunnable(epService, numEvents, i);
            threads[i] = new Thread(runnables[i]);
            threads[i].start();
        }

        // join
        log.info("Waiting for completion");
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
            assertNull(runnables[i].getException());
        }

        // verify
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select varagg.theEvents as c0 from SupportBean_S0").addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EventBean event = listener.assertOneGetNewAndReset();
        SupportBean[] window = (SupportBean[]) event.get("c0");
        assertEquals(numThreads*3, window.length);
    }

    public static class WriteRunnable implements Runnable {

        private final EPServiceProvider epService;
        private final int numEvents;
        private final int threadNum;

        private RuntimeException exception;

        public WriteRunnable(EPServiceProvider epService, int numEvents, int threadNum) {
            this.epService = epService;
            this.numEvents = numEvents;
            this.threadNum = threadNum;
        }

        public void run() {
            log.info("Started event send for write");

            try {
                String eplInto = "into table varagg select window(*) as theEvents from SupportBean(theString='E" + threadNum + "')#length(3)";
                epService.getEPAdministrator().createEPL(eplInto);

                for (int i = 0; i < numEvents; i++) {
                    epService.getEPRuntime().sendEvent(new SupportBean("E" + threadNum, i));
                }
            }
            catch (RuntimeException ex) {
                log.error("Exception encountered: " + ex.getMessage(), ex);
                exception = ex;
            }

            log.info("Completed event send for write");
        }

        public RuntimeException getException() {
            return exception;
        }
    }
}
