package util;

import static java.lang.System.out;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Helper 
{	
	public static <I, R> List<R> map(List<I> items, Function<I, R> fun)
	{
		List<R> result = new ArrayList<R>();
		for (I item : items)
			result.add(fun.apply(item));
		return result;
	}
	
	
	/**
	 * Combines 2 lists of the same length by applying the given function to each pair of items in the 2 lists.
	 */
	public static <I, R> List<R> combineLists(List<I> l1, List<I> l2, Function2<I, R> fun)
	{
		if (l1.size() != l2.size())
			throw new IllegalArgumentException("Lists must be the same size. List 1 size: " + l1.size() 
					+ ", list 2 size: " + l2.size());
		List<R> result = new ArrayList<R>();
		for (int i = 0; i < l1.size(); i++)
			result.add(fun.apply(l1.get(i), l2.get(i)));
		return result;
	}
	
	/**
	 * Applies the given function to each item in the given list and returns only those for which
	 * the function returned true.
	 */
	public static <T> List<T> filter(List<T> list, Function<T, Boolean> fun)
	{
		List<T> result = new ArrayList<>();
		for (T item : list)
			if (fun.apply(item))
				result.add(item);
		return result;
	}
	
	public static <K, V extends Comparable<V>> K argmin(Map<K, V> map)
	{
		Map.Entry<K, V> minEntry = null;
		
		for (Map.Entry<K, V> entry : map.entrySet())
		{
		    if (minEntry == null || entry.getValue().compareTo(minEntry.getValue()) < 0)
		    {
		        minEntry = entry;
		    }
		}
		return minEntry.getKey();
	}

	public static String readFile(String path) 
	{
		try
		{
			Charset encoding = Charset.defaultCharset();
			byte[] encoded = Files.readAllBytes(Paths.get(path));
			return encoding.decode(ByteBuffer.wrap(encoded)).toString();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public static <K, V extends Comparable<V>> K argmax(Map<K, V> map)
	{
		Map.Entry<K, V> maxEntry = null;

		for (Map.Entry<K, V> entry : map.entrySet())
		{
		    if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
		    {
		        maxEntry = entry;
		    }
		}
		return maxEntry.getKey();
	}

	public static <K, V> K argmax(Map<K, V> map, Comparator<V> comparator)
	{
		Map.Entry<K, V> maxEntry = null;

		for (Map.Entry<K, V> entry : map.entrySet())
		{
		    if (maxEntry == null || comparator.compare(entry.getValue(), maxEntry.getValue()) > 0)
		    {
		        maxEntry = entry;
		    }
		}
		return maxEntry.getKey();
	}
	
	public static <K, V> K maxElement(Map<K, V> map, Comparator<V> comparator)
	{
		Map.Entry<K, V> maxEntry = null;

		for (Map.Entry<K, V> entry : map.entrySet())
		{
		    if (maxEntry == null || comparator.compare(entry.getValue(), maxEntry.getValue()) > 0)
		    {
		        maxEntry = entry;
		    }
		}
		return maxEntry.getKey();
	}

	private static DecimalFormat decimalFormat = new DecimalFormat("#.#####");

	public static String formatFloat(float d)
	{
		return decimalFormat.format(d);
	}

	public static void printMultiLine(Collection<?> c)
	{
		for (Object o : c)
		{
			out.println(o);
		}
	}
	
	public static String toStringWithSeparator(Collection<?> collection, String separator)
	{
		if (collection.isEmpty())
			return "";

		StringBuilder b = new StringBuilder();
		Iterator<?> it = collection.iterator();
		while(true)
		{
			b.append(it.next());
			if (it.hasNext())
			{
				b.append(separator);
			}
			else
			{
				break;
			}
		}
		return b.toString();
	}
	
	public static void processInParallel(List<Runnable> jobs)
	{
		List<Future<?>> futures = new ArrayList<Future<?>>();
		int numThreads = Runtime.getRuntime().availableProcessors();
		ExecutorService exService = Executors.newFixedThreadPool(numThreads);
		try
		{
			for (Runnable job : jobs)
			{
				futures.add(exService.submit(job));
			}
	
			for (int i : new Range(jobs.size()))
			{
				try
				{
					futures.get(i).get();
				}
				catch(ExecutionException e)
				{
					throw new RuntimeException(e);
				}
				catch(InterruptedException e)
				{
					throw new RuntimeException(e);
				}
			}
		}
		finally
		{
			exService.shutdown();
		}
	}
	
	public static <T> List<T> processInParallelAndGetResult(List<Callable<T>> jobs)
	{
		List<Future<T>> futures = new ArrayList<>();
		List<T> results = new ArrayList<>();
		int numThreads = Runtime.getRuntime().availableProcessors();
		ExecutorService exService = Executors.newFixedThreadPool(numThreads);
		try
		{
			for (Callable<T> job : jobs)
			{
				futures.add(exService.submit(job));
			}
	
			for (int i : new Range(jobs.size()))
			{
				try
				{
					T result = futures.get(i).get();
					results.add(result);
				}
				catch(ExecutionException e)
				{
					throw new RuntimeException(e);
				}
				catch(InterruptedException e)
				{
					throw new RuntimeException(e);
				}
			}
		}
		finally
		{
			exService.shutdown();
		}
		
		return results;
	}

	
	public static void writeToFile(String fileName, String contents)
	{
		try
		{	
			File file = new File(fileName);
			
			if (!file.exists()) 
			{
				file.createNewFile();
			}
			
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			
			bw.write(contents);
			
			bw.close();
		}
		catch(IOException ex)
		{
			System.out.println("Helper.writeToFile caught error: " + ex.getMessage());
		}
	}
	
}


