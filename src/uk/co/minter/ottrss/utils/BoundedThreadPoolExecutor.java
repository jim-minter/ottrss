package uk.co.minter.ottrss.utils;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BoundedThreadPoolExecutor {
	private ThreadPoolExecutor threadPoolExecutor;
	private Semaphore semaphore;

	public BoundedThreadPoolExecutor(int bound) {
		threadPoolExecutor = new ThreadPoolExecutor(bound, bound, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
		semaphore = new Semaphore(bound);
	}

	public void execute(final Runnable command) throws Exception {
		semaphore.acquire();
		try {
			threadPoolExecutor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						command.run();
					} finally {
						semaphore.release();
					}
				}
			});
		} catch(Exception e) {
			semaphore.release();
			throw e;
		}
	}

	public void shutdown() {
		threadPoolExecutor.shutdown();
	}

	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return threadPoolExecutor.awaitTermination(timeout, unit);
	}
}
