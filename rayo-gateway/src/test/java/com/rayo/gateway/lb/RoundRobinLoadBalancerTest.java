package com.rayo.gateway.lb;


public abstract class RoundRobinLoadBalancerTest extends LoadBalancingTest {

	@Override
	GatewayLoadBalancingStrategy getLoadBalancer() {

		return new RoundRobinLoadBalancer();
	}
}
