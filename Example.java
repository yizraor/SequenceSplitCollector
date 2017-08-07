import java.util.*;
import java.util.stream.*;

public class Example
{
	public static void main(String[] args)
	{
		System.out.println("DEMO 1:");
		Stream.of("word", "hello", "dude", "word", "and word2", "stackoverflow", "question", "ask", "word", "example")
			.sequential()
			.collect(SequenceSplitCollector.forStrings("word", true, false))
			.forEach(list -> System.out.println("  " + list.toString()));
		
		System.out.println("DEMO 2:");
		Stream.of("word", "hello", "dude", "Word", "and word2", "stackoverflow", "question", "ask", "WORD", "example")
			.parallel()
			.collect(SequenceSplitCollector.forStrings("word", false, true))
			.forEach(list -> System.out.println("  " + list.toString()));
		
		System.out.println("DEMO 3:");
		Stream.of("The", "first", "sentence.", "And", "the", "second", "sentence.", "Finally", "the", "last", "sentence.")
			.collect(SequenceSplitCollector.of(s -> Character.isUpperCase(s.charAt(0)), false))
			.forEach(list -> System.out.println("  " + list.toString()));
		
		System.out.println("DEMO 4:");
		IntStream.range(0, 28).mapToObj(Integer::valueOf)
			.collect(SequenceSplitCollector.of(i -> i % 7 == 0, false))
			.forEach(list -> System.out.println("  " + list.toString()));
		
		System.out.println("DEMO 5:");
		IntStream.range(0, 28).mapToObj(i -> new AbstractMap.SimpleEntry<>(i, "str_" + Integer.toString(i)))
			.collect(SequenceSplitCollector.of(v -> v.getValue().contains("7"), true))
			.forEach(list -> System.out.println("  " + list.toString()));
	}
}
