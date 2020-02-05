# 멀티쓰레드 경매 서버

프로그래밍 언어 기술 및 패러다임 클래스에서 thread-safe한 경매 서버 시뮬레이터를 구현해 보았습니다.

## Thread-safe (Internal Sync) 서버

- 서버를 만들어 놓고, 서버의 메소드가 요청되면 메소드 내에서 서버에 있는 오브젝트를 이용하여 sync함.
- Thread-compatible(external sync)와의 차이:
    - External sync 일 경우 서버에 요청을 하기 전 sync를 실행해야함.

## Client/Server 모델 차용


### - Client: Runnable 클래스를 상속하여 모든 이벤트가 concurrent할 수 있게 함.

1. Sellers
    - 판매할 물품 서버에 업로드
2. Bidders (Aggressive, Conservative 두 타입의 입찰자 존재)
    - 공통
        - 현재 경매에 올라온 매물 리스트 확인
        - 물품의 가격을 확인
        - 물품에 입찰
        - 입찰 결과 확인
    - Aggressive
        - 물품 하나에 대한 입찰시 당시 잔고를 모두 입찰함. (다른 물품을 입찰하고, 두개 다 이길 경우를 고려하지 않음)
        - 만약 입찰한 물품들이 상당수 경매에서 이길 경우 잔고보다 큰 금액을 지불해야 할 수 있음

            → InsufficientFundException

    - Conservative
        - 모든 입찰이 이길 경우를 대비하며 잔고를 여러 입찰에 분배함

### - Server

- Seller와 Bidder가 요청하는 명령을 수행함.
- Internal synchronization을 위해 instance lock을 사용

    AuctionServer.java

        private Object instanceLock = new Object();
        
        ..
        public int submitItem(String sellerName, String itemName, int lowestBiddingPrice, int biddingDurationMs)
        	{
        	synchronized (instanceLock) {
            ..

- ServerPrinter.java
    - ServerPrinter의 각 함수는 AuctionServer을 상속한 뒤, super로 AuctionServer의 메소드를 수행한 후 Logger을 사용하여 logging함.

            @Override
                public int soldItemsCount()
                {
                    Logger.getInstance().logStart("soldItemsCount");

                    int returnValue = super.soldItemsCount(); //AuctionServer실행

                    Logger.getInstance().logEnd("soldItemsCount", returnValue);

                    return returnValue;
                }

- Logger.java
    - System.out을 instance lock으로 사용하여 thread-safe 유지.

            private PrintStream outStream = System.out;

            public void setOutput(PrintStream outStream)
            {
                this.outStream = outStream;
            }

            ...

            public void logStart(String method, Object... args)
            {
                synchronized (this.outStream)
                {
                        ...
                        this.outStream.format("S [%s] %s(%s)\n", formatter.format(now), method, argStr);
                  this.outStream.flush();
                    }
            }
