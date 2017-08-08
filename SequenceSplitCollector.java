import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Contains implementation for {@link java.util.stream.Collector} that split input stream into several subsequences.<br>
 * The algorithm checks each element of input stream in order to determine whether it is starting point of new subsequence or not.
 * Each subsequence being saved to separate 'inner' collection object.<br>
 * When input stream is fully processed, this implementation forms the 'outer' collection from all subsequences found.<br>
 * <br>
 * <h3>Example 1</h3><br>
 * Code:<br>
 * <pre>{@code
IntStream.range(0, 28).mapToObj(Integer::valueOf)
	.collect(SequenceSplitCollector.of(i -> i % 7 == 0, false))
	.forEach(list -> System.out.println(list.toString()));}</pre>
 * Output:<br>
 *  <pre>{@code
[0, 1, 2, 3, 4, 5, 6]
[7, 8, 9, 10, 11, 12, 13]
[14, 15, 16, 17, 18, 19, 20]
[21, 22, 23, 24, 25, 26, 27]}</pre>
 * <br>
 * This example uses the sequence of <code>Integer</code> values from 0 to 27 as input stream.<br>
 * Collector given to <code>collect()</code> method splits the input sequence into 4 subsequences.<br>
 * Each resulting subsequence starts from value that divides by 7 without rest (i % 7 == 0).<br>
 * <br>
 * <h3>Example 2</h3><br>
 * Code:<br>
 * <pre>{@code
Stream.of(":", "first", "subsequence", ": this is", "second", "subsequence", ":", "the", "last", "subsequence")
	.collect(SequenceSplitCollector.forStrings(":", true, false, SequenceSplitCollector.StringComparison.STARTS_FROM_SPLITTER))
	.forEach(list -> System.out.println(list.toString()));}</pre>
 * Output:<br>
 * <pre>{@code
[first, subsequence]
[second, subsequence]
[the, last, subsequence]}</pre>
 * <br>
 * This example uses the sequence of <code>String</code> values as an input stream.<br>
 * Collector given to <code>collect()</code> method splits the input sequence into 3 subsequences.<br>
 * Each resulting subsequence started when found a string value that starts from 'splitter' value ":".<br>
 * The string values that start new subsequences was not included into resulting data because of the 3-rd parameter of <code>SequenceSplitCollector::forStrings()</code> call at this example.<br>
 * 
 * @see java.util.stream.Stream
 * @see java.util.stream.Collector
 */
public final class SequenceSplitCollector
{
	/**
	 * Private constructor. Must not be called.
	 * @throws InstantiationError
	 */
	private SequenceSplitCollector() throws InstantiationError
	{
		throw new InstantiationError("this is static class");
	}
	
	/**
	 * Implements accumulator object for SequenceSplitCollector.
	 * @param <T> is the type of input stream elements
	 * @param <K> is the type of resulting inner collections
	 * @param <L> is the type of resulting outer collection
	 */
	private static class Accum<T, K extends Collection<? super T>, L extends List<K>>
	{
		private final List<T> buffer = new ArrayList<>();
		private final L data;
		private final Function<? super T, Boolean> split_detector;
		private final boolean exclude_splitter;
		private final Supplier<K> inner_collection_supplier;
		
		/**
		 * Constructs accumulator object using supplied parameters.
		 * @param split_detector is function that determines the starting point for new subsequence
		 * @param exclude_splitter when <b><code>true</code></b>: elements that start new subsequence will not be included into resulting data
		 * @param inner_collection_supplier is function that creates new instance for resulting inner collection
		 * @param outer_collection_supplier is function that creates new instance for resulting outer collection
		 */
		public Accum(Function<? super T, Boolean> split_detector, boolean exclude_splitter, Supplier<K> inner_collection_supplier, Supplier<L> outer_collection_supplier)
		{
			this.split_detector = split_detector;
			this.exclude_splitter = exclude_splitter;
			this.inner_collection_supplier = inner_collection_supplier;
			this.data = outer_collection_supplier.get();
		}
		
		/**
		 * Receives the next element of input stream.
		 * @param element is the element of input stream
		 * @return <code>this</code> object
		 */
		public Accum<T,K,L> pushElement(T element)
		{
			if ( split_detector.apply(element) )
			{
				K new_list = inner_collection_supplier.get();
				if ( ! exclude_splitter )
					new_list.add(element);
				data.add(new_list);
			}
			else if ( data.size() > 0 )
				data.get(data.size() - 1).add(element);
			else
				buffer.add(element);
			
			return this;
		}
		/**
		 * Combines <code>this</code> accumulator with another.<br>
		 * Order is significant: <code>this</code> accumulator is first, and another is second.
		 * @param another is accumulator to be combined with <code>this</code> object
		 * @return the resulting accumulator that contain all data from both source accumulators
		 */
		public Accum<T,K,L> combineWith(Accum<T,K,L> another)
		{
			if ( data.size() > 0 )
			{
				data.get(data.size() - 1).addAll(another.buffer);
				data.addAll(another.data);
			}
			else
			{
				buffer.addAll(another.buffer);
				data.addAll(another.data);
			}
			
			return this;
		}
		
		/**
		 * Constructs and returns the resulting data object.<br>
		 * Finishes the input stream processing.
		 * @return resulting 'outer' collection
		 */
		public L getResults()
		{
			if ( ! buffer.isEmpty() )
			{
				K new_list = inner_collection_supplier.get();
				new_list.addAll(buffer);
				data.add(0, new_list);
			}
			
			return data;
		}
	}
	
	/**
	 * The simplest form of constructor for desired {@link java.util.stream.Collector}.<br>
	 * Gathers each subsequence into <code>{@link java.util.List}&lt;T&gt;</code>, and then forms <code>List&lt;List&lt;T&gt;&gt;</code> from all subsequences found.
	 * 
	 * @param <T> is type of input stream elements
	 * @param split_detector is function that determines the starting point for new subsequence
	 * @param exclude_splitter when <b><code>true</code></b>: elements that start new subsequence will not be included into resulting data
	 * @return new {@link java.util.stream.Collector} object
	 */
	public static<T> Collector<T, ?, List<List<T>>> of(Function<? super T, Boolean> split_detector, boolean exclude_splitter)
	{
		return of(split_detector, exclude_splitter, ArrayList<T>::new, ArrayList<List<T>>::new);
	}
	
	/**
	 * Constructs the {@link java.util.stream.Collector} object that gather resulting subsequences to <code>{@link java.util.List}&lt;{@link java.util.Collection}&lt;T&gt;&gt;</code> using the supplied constructor for 'inner' <code>Collection</code>.
	 * @param <T> is type of input stream elements
	 * @param <K> is type of resulting 'inner' collection
	 * @param split_detector is function that determines the starting point for new subsequence
	 * @param exclude_splitter when <b><code>true</code></b>: elements that start new subsequence will not be included into resulting data
	 * @param inner_collection_supplier is function that creates new instance for resulting 'inner' collection
	 * @return new {@link java.util.stream.Collector} object
	 */
	public static<T, K extends Collection<? super T>> Collector<T, ?, List<K>> of(Function<? super T, Boolean> split_detector, boolean exclude_splitter, Supplier<K> inner_collection_supplier)
	{
		return of(split_detector, exclude_splitter, inner_collection_supplier, ArrayList<K>::new);
	}
	
	/**
	 * The most parameterized constructor for creation of desired {@link java.util.stream.Collector}.
	 * @param <T> is type of input stream elements
	 * @param <K> is type of resulting 'inner' collection
	 * @param <L> is type of resulting 'outer' collection
	 * @param split_detector is function that determines the starting point for new subsequence
	 * @param exclude_splitter when <b><code>true</code></b>: elements that start new subsequence will not be included into resulting data
	 * @param inner_collection_supplier is function that creates new instance for resulting 'inner' collection
	 * @param outer_collection_supplier is function that creates new instance for resulting 'outer' collection
	 * @return new {@link java.util.stream.Collector} object
	 */
	public static<T, K extends Collection<? super T>, L extends List<K>> Collector<T, ?, L> of(Function<? super T, Boolean> split_detector, boolean exclude_splitter, Supplier<K> inner_collection_supplier, Supplier<L> outer_collection_supplier)
	{
		Objects.requireNonNull(split_detector, "'split_detector' must not be null");
		Objects.requireNonNull(inner_collection_supplier, "'inner_collection_supplier' must not be null");
		Objects.requireNonNull(outer_collection_supplier, "'outer_collection_supplier' must not be null");

		return Collector.of
		(
			() -> new Accum<>(split_detector, exclude_splitter, inner_collection_supplier, outer_collection_supplier),
			Accum<T,K,L>::pushElement,
			Accum<T,K,L>::combineWith,
			Accum<T,K,L>::getResults
		);
	}
	
	/**
	 * Describes the string comparison kinds used by <code>{@link SequenceSplitCollector}::forStrings()</code> methods
	 */
	public enum StringComparison
	{
		/**
		 * Each string from input stream compared with 'splitter' by equality
		 */
		EQUALITY,
		/**
		 * Each string from input stream checked to be started with 'splitter' value
		 */
		STARTS_FROM_SPLITTER,
		/**
		 * Each string from input stream checked to be ended with 'splitter' value
		 */
		ENDS_WITH_SPLITTER,
		/**
		 * Each string from input stream checked to be contained 'splitter' value
		 */
		CONTAINS_SPLITTER
	}
	
	/**
	 * The simplest version of <code>SequenceSplitCollector::forStrings()</code>.<br>
	 * Works for <code>{@link java.util.stream.Stream}&lt;String&gt;</code>.<br>
	 * Compares each input element with 'splitter' <i>by equality</i> in order of detecting the new subsequences.<br>
	 * Gathers each subsequence into <code>{@link java.util.List}&lt;String&gt;</code>, and then forms <code>List&lt;List&lt;String&gt;&gt;</code> consisting of all subsequences found.<br>
	 * Is equivalent to: <code>SequenceSplitCollector.forStrings(splitter, exclude_splitter, ignore_case, SequenceSplitCollector.StringComparison.EQUALITY)</code>.
	 * @param splitter is string value that starts new subsequence
	 * @param exclude_splitter when <b><code>true</code></b>: elements that start new subsequence will not be included into resulting data
	 * @param ignore_case when <b><code>true</code></b>: strings compared without case sensitivity
	 * @return new {@link java.util.stream.Collector} object
	 */
	public static Collector<String, ?, List<List<String>>> forStrings(String splitter, boolean exclude_splitter, boolean ignore_case)
	{
		return forStrings(splitter, exclude_splitter, ignore_case, StringComparison.EQUALITY, ArrayList<String>::new, ArrayList<List<String>>::new);
	}
	
	/**
	 * The simple version of <code>SequenceSplitCollector::forStrings()</code>.<br>
	 * Works for <code>{@link java.util.stream.Stream}&lt;String&gt;</code>.<br>
	 * Compares each input element with 'splitter' in order of detecting the new subsequences.<br>
	 * Gathers each subsequence into <code>{@link java.util.List}&lt;String&gt;</code>, and then forms <code>List&lt;List&lt;String&gt;&gt;</code> from all subsequences found.
	 * @param splitter is string value that starts new subsequence
	 * @param exclude_splitter when <b><code>true</code></b>: elements that start new subsequence will not be included into resulting data
	 * @param ignore_case when <b><code>true</code></b>: strings compared without case sensitivity
	 * @param comparison is kind of string comparison
	 * @return new {@link java.util.stream.Collector} object
	 * @see SequenceSplitCollector.StringComparison
	 */
	public static Collector<String, ?, List<List<String>>> forStrings(String splitter, boolean exclude_splitter, boolean ignore_case, StringComparison comparison)
	{
		return forStrings(splitter, exclude_splitter, ignore_case, comparison, ArrayList<String>::new, ArrayList<List<String>>::new);
	}
	
	/**
	 * Constructs {@link java.util.stream.Collector} that gathers resulting subsequences to <code>{@link java.util.List}&lt;{@link java.util.Collection}&lt;String&gt;&gt;</code> using the supplied constructor for 'inner' Collection.<br>
	 * Works for <code>{@link java.util.stream.Stream}&lt;String&gt;</code>.<br>
	 * Compares each input element with 'splitter' in order of detecting the new subsequences.
	 * @param <K> is type of resulting 'inner' collection
	 * @param splitter is string value that starts new subsequence
	 * @param exclude_splitter when <b><code>true</code></b>: elements that start new subsequence will not be included into resulting data
	 * @param ignore_case when <b><code>true</code></b>: strings compared without case sensitivity
	 * @param comparison is kind of string comparison
	 * @param inner_collection_supplier is function that creates new instance for resulting 'inner' collection
	 * @return new {@link java.util.stream.Collector} object
	 * @see SequenceSplitCollector.StringComparison
	 */
	public static<K extends Collection<? super String>> Collector<String, ?, List<K>> forStrings(String splitter, boolean exclude_splitter, boolean ignore_case, StringComparison comparison, Supplier<K> inner_collection_supplier)
	{
		return forStrings(splitter, exclude_splitter, ignore_case, comparison, inner_collection_supplier, ArrayList<K>::new);
	}
	
	/**
	 * The most parameterized version of <code>SequenceSplitCollector::forStrings()</code>.<br>
	 * Works for <code>{@link java.util.stream.Stream}&lt;String&gt;</code>.<br>
	 * Compares each input element with 'splitter' in order of detecting the new subsequences.
	 * @param <K> is type of resulting 'inner' collection
	 * @param <L> is type of resulting 'outer' collection
	 * @param splitter is string value that starts new subsequence
	 * @param exclude_splitter when <b><code>true</code></b>: elements that start new subsequence will not be included into resulting data
	 * @param ignore_case when <b><code>true</code></b>: strings compared withoud case sensitivity
	 * @param comparison is kind of string comparison
	 * @param inner_collection_supplier is function that creates new instance for resulting 'inner' collection
	 * @param outer_collection_supplier is function that creates new instance for resulting 'outer' collection
	 * @return new {@link java.util.stream.Collector} object
	 * @see SequenceSplitCollector.StringComparison
	 */
	public static<K extends Collection<? super String>, L extends List<K>> Collector<String, ?, L> forStrings(String splitter, boolean exclude_splitter, boolean ignore_case, StringComparison comparison, Supplier<K> inner_collection_supplier, Supplier<L> outer_collection_supplier)
	{
		Objects.requireNonNull(splitter, "'splitter' must not be null");
		Objects.requireNonNull(inner_collection_supplier, "'inner_collection_supplier' must not be null");
		Objects.requireNonNull(outer_collection_supplier, "'outer_collection_supplier' must not be null");
		
		if ( splitter.length() == 0 )
			throw new IllegalArgumentException("'splitter' must not be empty");
		
		String splitter_upcase = splitter.toUpperCase();
		Function<String, Boolean> split_detector;
		
		switch ( comparison )
		{
			case EQUALITY:
				split_detector =
					ignore_case ?
					s -> s.equalsIgnoreCase(splitter) :
					s -> s.equals(splitter);
				break;
			case STARTS_FROM_SPLITTER:
				split_detector =
					ignore_case ?
					s -> s.toUpperCase().startsWith(splitter_upcase) :
					s -> s.startsWith(splitter);
				break;
			case ENDS_WITH_SPLITTER:
				split_detector =
					ignore_case ?
					s -> s.toUpperCase().endsWith(splitter_upcase) :
					s -> s.endsWith(splitter);
				break;
			case CONTAINS_SPLITTER:
				split_detector =
					ignore_case ?
					s -> s.toUpperCase().contains(splitter_upcase) :
					s -> s.contains(splitter);
			default:
				throw new IllegalArgumentException("'comparison' value is unknown");
		}
		
		return of(split_detector, exclude_splitter, inner_collection_supplier, outer_collection_supplier);
	}
}