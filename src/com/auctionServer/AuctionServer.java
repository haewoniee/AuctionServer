package com.auctionServer;

/**
 *  @author $Haewon Han
 *  Last Updated 2-6-2017
 */


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;



public class AuctionServer
{
	/**
	 * Singleton: the following code makes the server a Singleton. You should
	 * not edit the code in the following noted section.
	 * 
	 * For test purposes, we made the constructor protected. 
	 */

	/* Singleton: Begin code that you SHOULD NOT CHANGE! */
	protected AuctionServer()
	{
	}

	private static AuctionServer instance = new AuctionServer();

	public static AuctionServer getInstance()
	{
		return instance;
	}

	/* Singleton: End code that you SHOULD NOT CHANGE! */





	/* Statistic variables and server constants: Begin code you should likely leave alone. */


	/**
	 * Server statistic variables and access methods:
	 */
	private int soldItemsCount = 0;
	private int revenue = 0;
	private int uncollectedRevenue = 0;

	public int soldItemsCount()
	{
		synchronized (instanceLock) {
			return this.soldItemsCount;
		}
	}

	public int revenue()
	{
		synchronized (instanceLock) {
			return this.revenue;
		}
	}
	
	public int uncollectedRevenue () {
		synchronized (instanceLock) {
			return this.uncollectedRevenue;
		}
	}



	/**
	 * Server restriction constants:
	 */
	public static final int maxBidCount = 10; // The maximum number of bids at any given time for a buyer.
	public static final int maxSellerItems = 20; // The maximum number of items that a seller can submit at any given time.
	public static final int serverCapacity = 80; // The maximum number of active items at a given time.


	/* Statistic variables and server constants: End code you should likely leave alone. */



	/**
	 * Some variables we think will be of potential use as you implement the server...
	 */

	// List of items currently up for bidding (will eventually remove things that have expired).
	private List<Item> itemsUpForBidding = new ArrayList<Item>();
	

	// The last value used as a listing ID.  We'll assume the first thing added gets a listing ID of 0.
	private int lastListingID = -1; 

	// List of item IDs and actual items.  This is a running list with everything ever added to the auction.
	private HashMap<Integer, Item> itemsAndIDs = new HashMap<Integer, Item>();

	// List of itemIDs and the highest bid for each item.  This is a running list with everything ever bid upon.
	private HashMap<Integer, Integer> highestBids = new HashMap<Integer, Integer>();

	// List of itemIDs and the person who made the highest bid for each item.   This is a running list with everything ever bid upon.
	private HashMap<Integer, String> highestBidders = new HashMap<Integer, String>(); 
	
	// List of Bidders who have been permanently banned because they failed to pay the amount they promised for an item. 
	private HashSet<String> blacklist = new HashSet<String>();
	
	// List of sellers and how many items they have currently up for bidding.
	private HashMap<String, Integer> itemsPerSeller = new HashMap<String, Integer>();

	// List of buyers and how many items on which they are currently bidding.
	private HashMap<String, Integer> itemsPerBuyer = new HashMap<String, Integer>();

	// List of itemIDs that have been paid for. This is a running list including everything ever paid for.
	private HashSet<Integer> itemsSold = new HashSet<Integer> ();

	// Object used for instance synchronization if you need to do it at some point 
	// since as a good practice we don't use synchronized (this) if we are doing internal
	// synchronization.
	//
	private Object instanceLock = new Object(); 









	/*
	 *  The code from this point forward can and should be changed to correctly and safely 
	 *  implement the methods as needed to create a working multi-threaded server for the 
	 *  system.  If you need to add Object instances here to use for locking, place a comment
	 *  with them saying what they represent.  Note that if they just represent one structure
	 *  then you should probably be using that structure's intrinsic lock.
	 */


	/**
	 * Attempt to submit an <code>Item</code> to the auction
	 * @param sellerName Name of the <code>Seller</code>
	 * @param itemName Name of the <code>Item</code>
	 * @param lowestBiddingPrice Opening price
	 * @param biddingDurationMs Bidding duration in milliseconds
	 * @return A positive, unique listing ID if the <code>Item</code> listed successfully, otherwise -1
	 */
	public int submitItem(String sellerName, String itemName, int lowestBiddingPrice, int biddingDurationMs)
	{
		// TOCHECK
		// Some reminders:
		//   Make sure there's room in the auction site.
		//   If the seller is a new one, add them to the list of sellers.
		//   If the seller has too many items up for bidding, don't let them add this one.
		//   Don't forget to increment the number of things the seller has currently listed.
		synchronized (instanceLock) {
			if (itemsUpForBidding.size() < serverCapacity
					&& (!itemsPerSeller.containsKey(sellerName) || itemsPerSeller.get(sellerName) < maxSellerItems))
			{
				if (!itemsPerSeller.containsKey(sellerName))
				{
					itemsPerSeller.put(sellerName, 0);
				}
				int numSelling = itemsPerSeller.get(sellerName);

				numSelling++;
				lastListingID++;

				Item newItem = new Item(sellerName, itemName, lastListingID, lowestBiddingPrice, biddingDurationMs);
					
				itemsPerSeller.replace(sellerName, numSelling);
				itemsAndIDs.put(lastListingID, newItem);
				itemsUpForBidding.add(newItem);
				highestBids.put(lastListingID, lowestBiddingPrice);
				highestBidders.put(lastListingID, null);
				
				return lastListingID;

			}		
			return -1;

		}
		
	}

	/**
	 * Get all <code>Items</code> active in the auction
	 * @return A copy of the <code>List</code> of <code>Items</code>
	 */
	public List<Item> getItems()
	{
		// TOCHECK
		// Some reminders:
		//    Don't forget that whatever you return is now outside of your control.
		synchronized (instanceLock) {
			return new ArrayList<Item>(itemsUpForBidding);
		}
	}


	/**
	 * Attempt to submit a bid for an <code>Item</code>
	 * @param bidderName Name of the <code>Bidder</code>
	 * @param listingID Unique ID of the <code>Item</code>
	 * @param biddingAmount Total amount to bid
	 * @return True if successfully bid, false otherwise
	 */
	public boolean submitBid(String bidderName, int listingID, int biddingAmount)
	{
		// TOCHECK
		// Some reminders:
		//    See if the item exists.
		//    See if it can be bid upon.
		//    See if this bidder has too many items in their bidding list.
		//    Make sure the bidder has not been blacklisted
		//    Get current bidding info.
		//    See if they already hold the highest bid.
		//    See if the new bid isn't better than the existing/opening bid floor.
		//    Decrement the former winning bidder's count
		//   Put your bid in place
		synchronized (instanceLock)
		{
			Item itemToBid = itemsAndIDs.get(listingID);
			if (itemToBid != null && itemToBid.biddingOpen()
					&& itemsUpForBidding.contains(itemToBid)
					&& (!itemsPerBuyer.containsKey(bidderName) || itemsPerBuyer.get(bidderName) < maxBidCount)
					&& !blacklist.contains(bidderName)
					&& (highestBidders.get(listingID) == null || highestBidders.get(listingID).equals(bidderName))
					&& (highestBids.get(listingID) < biddingAmount))
			{
				//   Decrement the former winning bidder's count
				String formerBidder = highestBidders.get(listingID);
				if (formerBidder != null)
				{
					itemsPerBuyer.replace(formerBidder, itemsPerBuyer.get(formerBidder) - 1);
				}
				
				//	Add the bidder to the list if not yet listed
				if (!itemsPerBuyer.containsKey(bidderName))
				{
					itemsPerBuyer.put(bidderName, 0);
				}
				
				//	Increment the bidder's count
				itemsPerBuyer.put(bidderName, itemsPerBuyer.get(bidderName) + 1);
				
				highestBids.put(listingID, biddingAmount);
				highestBidders.put(listingID, bidderName);
				return true;
			}
			
			return false;

		}
	}

	/**
	 * Check the status of a <code>Bidder</code>'s bid on an <code>Item</code>
	 * @param bidderName Name of <code>Bidder</code>
	 * @param listingID Unique ID of the <code>Item</code>
	 * @return 1 (success) if bid is over and this <code>Bidder</code> has won<br>
	 * 2 (open) if this <code>Item</code> is still up for auction<br>
	 * 3 (failed) If this <code>Bidder</code> did not win or the <code>Item</code> does not exist
	 */
	public int checkBidStatus(String bidderName, int listingID)
	{
		final int SUCCESS = 1, OPEN = 2, FAILURE = 3;
		// TOCHECK
		// Some reminders:
		//   If the bidding is closed, clean up for that item.
		//     Remove item from the list of things up for bidding.
		//     Decrease the count of items being bid on by the winning bidder if there was any...
		//     Update the number of open bids for this seller
		//     If the item was sold to someone, update the uncollectedRevenue field appropriately
		synchronized (instanceLock)
		{
			Item itemToCheck = itemsAndIDs.get(listingID);
			if (itemToCheck != null && itemToCheck.biddingOpen())
			{
				return OPEN;
			} else {
				//     Remove item from the list of things up for bidding.
				if (itemsUpForBidding.contains(itemToCheck))
				{
					itemsUpForBidding.remove(itemToCheck);
					//	Decrease the count of items being bid on by the winning bidder
					String hBidder = highestBidders.get(listingID);

					if (hBidder != null)
					{
						itemsSold.add(listingID);
						uncollectedRevenue += highestBids.get(listingID);
						
						itemsPerBuyer.put(hBidder, itemsPerBuyer.get(hBidder) - 1);
						itemsPerSeller.put(itemToCheck.seller(), itemsPerSeller.get(itemToCheck.seller()) - 1);
						if (hBidder.equals(bidderName)) {
							return SUCCESS;
						} else {
							return FAILURE;
						}

					}
					
				}
				
			}
			return FAILURE;

		}

	}

	/**
	 * Check the current bid for an <code>Item</code>
	 * @param listingID Unique ID of the <code>Item</code>
	 * @return The highest bid so far or the opening price if there is no bid on the <code>Item</code>,
	 * or -1 if no <code>Item</code> with the given listingID exists
	 */
	public int itemPrice(int listingID)
	{
		// TOCHECK
		// Remember: once an item has been purchased, this method should continue to return the
		// highest bid, even if the buyer paid more than necessary for the item or if the buyer
		// is subsequently blacklisted
		
		synchronized (instanceLock)
		{
			if (itemsAndIDs.containsKey(listingID))
			{
				if (highestBids.containsKey(listingID)) {
					return highestBids.get(listingID);
				} else {
					return itemsAndIDs.get(listingID).lowestBiddingPrice();
				}
			}
			return -1;

		}
	}

	/**
	 * Check whether an <code>Item</code> has a bid on it
	 * @param listingID Unique ID of the <code>Item</code>
	 * @return True if there is no bid or the <code>Item</code> does not exist, false otherwise
	 */
	public boolean itemUnbid(int listingID)
	{
		// TOCHECK
		synchronized (instanceLock)
		{
			Item itemToCheck = itemsAndIDs.get(listingID);
			if (itemToCheck != null)
			{
				if (itemsUpForBidding.contains(itemToCheck))
				{
					return highestBids.containsKey(listingID);
				}
			}
			return true;

		}
	}

	/**
	 * Pay for an <code>Item</code> that has already been won.
	 * @param bidderName Name of <code>Bidder</code>
	 * @param listingID Unique ID of the <code>Item</code>
	 * @param amount The amount the <code>Bidder</code> is paying for the item 
	 * @return The name of the <code>Item</code> won, or null if the <code>Item</code> was not won by the <code>Bidder</code> or if the <code>Item</code> did not exist
	 * @throws InsufficientFundsException If the <code>Bidder</code> did not pay at least the final selling price for the <code>Item</code>
	 */
	public String payForItem (String bidderName, int listingID, int amount) throws InsufficientFundsException {
		// TOCHECK
		// Remember:
		// - Check to make sure the buyer is the correct individual and can afford the item
		// - If the purchase is valid, update soldItemsCount, revenue, and uncollectedRevenue
		// - If the amount tendered is insufficient, cancel all active bids held by the buyer, 
		//   add the buyer to the blacklist, and throw an InsufficientFundsException 
		synchronized (instanceLock)
		{
			String hBidder = highestBidders.get(listingID);

			if (hBidder != null && hBidder.equals(bidderName))
			{
				int finalPrice = highestBids.get(listingID);
				if (finalPrice <= amount)
				{
					soldItemsCount++;
					revenue += finalPrice;
					uncollectedRevenue -= finalPrice;
					return itemsAndIDs.get(listingID).name();
				}
				else
				{
					blacklist.add(bidderName);
					
					for (Map.Entry<Integer, String> i : highestBidders.entrySet()) {
						if (i.getValue() != null && i.getValue().equals(bidderName)) {
							highestBidders.remove(i.getKey());
							highestBids.remove(i.getKey());
						}
					}
					itemsPerBuyer.put(bidderName, 0);
					throw new InsufficientFundsException();
				}
			}
			return null;
		}
		
	}

}
