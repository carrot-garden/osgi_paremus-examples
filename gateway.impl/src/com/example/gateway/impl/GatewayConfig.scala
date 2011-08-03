package com.example.gateway.impl

import aQute.bnd.annotation.metatype.Meta.OCD

@OCD(id="com.example.gateway",factory=true)    
trait GatewayConfig {
	def id: String
}