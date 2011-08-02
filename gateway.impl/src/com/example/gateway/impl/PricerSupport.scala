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
package com.example.gateway.impl

import scala.collection.mutable.HashMap
import java.{ util => ju }
import collection.JavaConversions._
import com.example.gateway.PricingEngine
import util.Random.shuffle
import com.example.gateway.messages.QuoteRequest

import aQute.bnd.annotation.component.Reference

trait PricerSupport {
  private val pricers = new HashMap[PricingEngine, Map[String, Any]]

  @Reference(`type`='*')
  def addPricer(pricer: PricingEngine, attributes: ju.Map[String, Any]) {
    pricers.synchronized {
      pricers += pricer -> attributes.toMap
    }
  }

  def removePricer(pricer: PricingEngine) {
    pricers.synchronized {
      pricers -= pricer
    }
  }
  
  protected def gatewayID: String

  def submitPrice(clientID: String, quote: QuoteRequest): Boolean = {
    val id = gatewayID
    
    def findNextPricer(typeName: String): Option[PricingEngine] = {
      pricers.synchronized {
        val options = pricers.filter(_._2.exists(entry => entry._1 == PricingEngine.TYPE && entry._2 == typeName)).map(_._1)
        if (options.isEmpty) {
          None
        } else {
          Some(shuffle(options.toList).head)
        }
      }
    }
    
    val typeName = if (quote.isIndicative) PricingEngine.INDICATIVE_TYPE else PricingEngine.FIRM_TYPE
            
    findNextPricer(typeName) match {
      case Some(pricer) => {
        pricer.price(id, clientID, quote)
        true
      }
      case None => false
    }
  }

}