package com.auctionServer;

public interface Bidder extends Client {
	int cash();
	int cashSpent();
	int mostItemsAvailable();
}
