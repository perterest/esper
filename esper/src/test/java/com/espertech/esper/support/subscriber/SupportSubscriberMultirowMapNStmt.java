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

package com.espertech.esper.support.subscriber;

import java.util.Map;

public class SupportSubscriberMultirowMapNStmt extends SupportSubscriberMultirowMapBase
{
    public SupportSubscriberMultirowMapNStmt() {
        super(false);
    }

    public void update(Map[] newEvents, Map[] oldEvents) {
        addIndication(newEvents, oldEvents);
    }
}
