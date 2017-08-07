import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public final class SequenceSplitCollector
{
	private SequenceSplitCollector()
	{
		throw new InstantiationError("this is static class");
	}
	
	private static class Accum<T, K extends Collection<? super T>, L extends List<K>>
	{
		private final List<T> buffer = new ArrayList<>();
		private final L data;
		private final Function<? super T, Boolean> split_detector;
		private final boolean exclude_splitter;
		private final Supplier<K> inner_collection_supplier;
		
		Accum(Function<? super T, Boolean> split_detector, boolean exclude_splitter, Supplier<K> inner_collection_supplier, Supplier<L> outer_collection_supplier)
		{
			this.split_detector = split_detector;
			this.exclude_splitter = exclude_splitter;
			this.inner_collection_supplier = inner_collection_supplier;
			this.data = outer_collection_supplier.get();
		}
		
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
		
		public Accum<T,K,L> combineWith(Accum<T,K,L> another)
		{
		/*
			System.out.println("  # this.buffer = " + this.buffer.toString());
			System.out.println("  # this.data = " + this.data.toString());
			System.out.println("  # another.buffer = " + another.buffer.toString());
			System.out.println("  # another.data = " + another.data.toString());
		*/
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
	
	public static<T> Collector<T, ?, List<List<T>>> of(Function<? super T, Boolean> split_detector, boolean exclude_splitter)
	{
		return of(split_detector, exclude_splitter, ArrayList<T>::new, ArrayList<List<T>>::new);
	}
	
	public static<T, K extends Collection<? super T>> Collector<T, ?, List<K>> of(Function<? super T, Boolean> split_detector, boolean exclude_splitter, Supplier<K> inner_collection_supplier)
	{
		return of(split_detector, exclude_splitter, inner_collection_supplier, ArrayList<K>::new);
	}
	
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
	
	public static Collector<String, ?, List<List<String>>> forStrings(String splitter, boolean exclude_splitter, boolean ignore_case)
	{
		return forStrings(splitter, exclude_splitter, ignore_case, ArrayList<String>::new);
	}
	
	public static<K extends Collection<? super String>> Collector<String, ?, List<K>> forStrings(String splitter, boolean exclude_splitter, boolean ignore_case, Supplier<K> collection_supplier)
	{
		Objects.requireNonNull(splitter, "'splitter' must not be null");
		Objects.requireNonNull(collection_supplier, "'collection_supplier' must not be null");
		
		if ( splitter.length() == 0 )
			throw new IllegalArgumentException("'splitter' must not be empty");
		
		Function<String, Boolean> split_detector =
			ignore_case ?
			s -> s.equalsIgnoreCase(splitter) :
			s -> s.equals(splitter);
		
		return of(split_detector, exclude_splitter, collection_supplier, ArrayList<K>::new);
	}
}
