/* Copyright 2011 Paremus Limited
 * 
 * Licensed under the Apache License, Version 2.0 (the License)
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License. 
 * see http://www.apache.org/licenses/LICENSE-2.0 
 */

package com.example.gateway.messages;

public class Quote extends Message {
	private static final long serialVersionUID = 1L;
	private String underlying;
    private int price;
    private boolean indicative;

    public String getUnderlying() {
        return underlying;
    }

    public int getPrice() {
        return price;
    }

    public boolean isIndicative() {
        return indicative;
    }

    public Quote(Object id, String underlying, int price, boolean indicative) {
        super(id);
        this.underlying = underlying;
        this.price = price;
        this.indicative = indicative;
    }

    @Override
    public String toString() {
        return "Quote{" +
                "underlying='" + underlying + '\'' +
                ", price=" + price +
                ", indicative=" + indicative +
                '}';
    }


}
