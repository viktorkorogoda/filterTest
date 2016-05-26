package filter.test;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import filter.api.Filter;

/**
 * This is a small app that demonstrates how a {@link Filter} can be used.
 *
 * If you want to score some extra points you can implement JUnit tests for your
 * implementation.
 */
public class FilterTest {
	private static final int numberOfSignalsPerProducer = 100;
	private static final int numberOfSignalsProducers = 3;

	private static class AccessFilter implements Filter {
		private AtomicInteger maxCallCount = new AtomicInteger(0);
		private List<Long> callTimes = new LinkedList<Long>();

		/**
		 * @param N
		 *            maximum number of signals per last 100 seconds
		 */
		private AccessFilter(int N) {
			this.maxCallCount = new AtomicInteger(N);
		}

		@Override
		public synchronized boolean isSignalAllowed() {
			int period = 100;
			long currentCallTime = System.currentTimeMillis();
			long timeFrameStart = currentCallTime - period * 1000;
			
			if (callTimes.isEmpty()) {
				callTimes.add(currentCallTime);
				return true;
			}
			if (timeFrameStart < callTimes.get(0)) {
				if (callTimes.size() < maxCallCount.get()) {
					callTimes.add(currentCallTime);
					return true;
				} else {
					return false;
				}
			}
			
			Iterator<Long> callTimesIterator = callTimes.iterator();
			while(callTimesIterator.hasNext()) {
				long callTimesItem = callTimesIterator.next();
				
				if (timeFrameStart > callTimesItem) {
					callTimesIterator.remove();
					continue;
				} else {
					if (callTimes.size() < maxCallCount.get()) {
						callTimes.add(currentCallTime);
						return true;
					} else {
						return false;
					}
				}
			}

			return false;
		}
	}

	private static class TestProducer extends Thread {
		private final Filter filter;
		private final AtomicInteger totalPassed;

		private TestProducer(Filter filter, AtomicInteger totalPassed) {
			this.filter = filter;
			this.totalPassed = totalPassed;
		}

		@Override
		public void run() {
			final long startTime = System.currentTimeMillis();
			Random rnd = new Random();
			try {
				for (int j = 0; j < numberOfSignalsPerProducer; j++) {
					boolean isSignalAllowed = filter.isSignalAllowed();
					if (isSignalAllowed) 
						totalPassed.incrementAndGet();
					
					System.out.println(Thread.currentThread().getName() + " : [" + (System.currentTimeMillis() - startTime) / 1000 + "] : [" + totalPassed.get() + "] : recieved answer = " + isSignalAllowed + " " + j);
					Thread.sleep(rnd.nextInt(500));
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String... args) throws InterruptedException {
		final int N = 100;
		Filter filter = new AccessFilter(N);

		AtomicInteger totalPassed = new AtomicInteger();
		Thread[] producers = new Thread[numberOfSignalsProducers];
		for (int i = 0; i < producers.length; i++)
			producers[i] = new TestProducer(filter, totalPassed);

		for (Thread producer : producers)
			producer.start();

		for (Thread producer : producers)
			producer.join();

		System.out.println("Filter allowed " + totalPassed + " signals out of " + (numberOfSignalsPerProducer * numberOfSignalsProducers));
	}

}
