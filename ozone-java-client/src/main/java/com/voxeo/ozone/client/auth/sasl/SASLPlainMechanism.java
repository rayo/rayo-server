/**
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.voxeo.ozone.client.auth.sasl;

import com.voxeo.ozone.client.XmppConnection;
import com.voxeo.servlet.xmpp.ozone.stanza.sasl.AuthMechanism;

/**
 * Implementation of the SASL PLAIN mechanism
 *
 * @author Jay Kline
 */
public class SASLPlainMechanism extends SASLMechanism {

    public SASLPlainMechanism(XmppConnection connection) {
    	
        super(connection);
    }

    protected AuthMechanism.Type getName() {
    	
        return AuthMechanism.Type.PLAIN;
    }
}
