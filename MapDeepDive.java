import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;
import java.util.function.*;

/**
 * ============================================================
 *  MAP DEEP DIVE — HashMap, TreeMap, ConcurrentHashMap
 *  Run: javac MapDeepDive.java && java MapDeepDive
 * ============================================================
 *
 *  WHAT IS A MAP?
 *  --------------
 *  A Map stores key → value pairs.
 *
 *  Key characteristics:
 *    - Keys must be UNIQUE (duplicate key overwrites the old value)
 *    - Values CAN be duplicate
 *    - Retrieval is always by key
 *    - Not a Collection — Map does not extend Collection<E>
 *
 *  Main implementations:
 *    - HashMap           → hash table, O(1) avg, no order
 *    - TreeMap           → Red-Black Tree, O(log n), sorted key order
 *    - LinkedHashMap     → HashMap + linked list, O(1) avg, insertion order
 *    - ConcurrentHashMap → thread-safe HashMap, O(1) avg, no order
 *    - Hashtable         → legacy, fully synchronized, avoid in new code
 *
 *  Common Map methods:
 *    put(K, V)                   → insert or update, returns old value
 *    get(K)                      → returns value or null
 *    getOrDefault(K, V)          → returns value or default if absent
 *    remove(K)                   → removes entry, returns old value
 *    containsKey(K)              → O(1) for HashMap
 *    containsValue(V)            → O(n) always — scans all values
 *    size()                      → number of entries
 *    keySet()                    → Set view of all keys
 *    values()                    → Collection view of all values
 *    entrySet()                  → Set<Map.Entry<K,V>> — most efficient iteration
 *    putIfAbsent(K, V)           → only puts if key absent
 *    computeIfAbsent(K, fn)      → compute and put if key absent
 *    computeIfPresent(K, fn)     → compute and update if key present
 *    compute(K, fn)              → compute unconditionally
 *    merge(K, V, fn)             → merge value with existing
 *    forEach(BiConsumer)         → iterate all entries
 *    replaceAll(BiFunction)      → update all values in place
 */
public class MapDeepDive {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== MAP DEEP DIVE: HashMap, TreeMap, ConcurrentHashMap ===\n");

        section1_MapInterfaceBasics();
        section2_HashMapInternals();
        section3_HashMapResizing();
        section4_HashMapJava8Methods();
        section5_TreeMapBasics();
        section6_TreeMapNavigation();
        section7_TreeMapCustomComparator();
        section8_LinkedHashMap();
        section9_ConcurrentHashMap();
        section10_ConcurrentHashMapInternals();
        section11_FrequencyCounting();
        section12_RealWorldCaching();
        section13_RealWorldGrouping();
        section14_RealWorldAnagram();
        section15_CommonMistakes();
        section16_PerformanceBenchmark();
        section17_InterviewSummary();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 1 — Map Interface Basics
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * MAP INTERFACE
     * -------------
     * java.util.Map<K,V> is NOT a subtype of Collection.
     * It is its own hierarchy: Map → SortedMap → NavigableMap.
     *
     * put(K, V) returns the OLD value (or null if key was absent).
     * This return value is commonly ignored but is useful for detecting
     * whether a key already existed.
     *
     * null keys: HashMap allows one null key (hashed to bucket 0).
     *            TreeMap does NOT allow null keys.
     *            ConcurrentHashMap does NOT allow null keys or values.
     *
     * null values: HashMap / TreeMap allow null values.
     *              ConcurrentHashMap does NOT allow null values.
     *              (Null value is ambiguous in CHM: is key absent or value null?)
     */
    static void section1_MapInterfaceBasics() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 1: Map Interface Basics");
        System.out.println("─────────────────────────────────────────");

        // Program to the Map interface — not the implementation
        Map<String, Integer> map = new HashMap<>();

        // put() — returns old value (null if key was absent)
        System.out.println("put(Apple, 10):  " + map.put("Apple",  10)); // null (new key)
        System.out.println("put(Banana, 20): " + map.put("Banana", 20)); // null (new key)
        System.out.println("put(Apple, 15):  " + map.put("Apple",  15)); // 10   (old value returned)

        System.out.println("Map: " + map);
        System.out.println("Size: " + map.size()); // 2, not 3

        // get() — returns null if key absent (not an exception)
        System.out.println("\nget(Apple):  " + map.get("Apple"));   // 15
        System.out.println("get(Mango):  " + map.get("Mango"));    // null

        // getOrDefault() — safe alternative to null check
        System.out.println("getOrDefault(Mango, 0): " + map.getOrDefault("Mango", 0)); // 0

        // containsKey — O(1) for HashMap
        System.out.println("\ncontainsKey(Banana): " + map.containsKey("Banana")); // true
        System.out.println("containsKey(Grapes): " + map.containsKey("Grapes")); // false

        // containsValue — O(n) always — scans all entries
        System.out.println("containsValue(20): " + map.containsValue(20)); // true

        // remove() — returns old value
        System.out.println("\nremove(Banana): " + map.remove("Banana")); // 20
        System.out.println("remove(Ghost):  " + map.remove("Ghost"));   // null
        System.out.println("After removes: " + map);

        // putIfAbsent() — only puts if key not already present
        map.putIfAbsent("Apple", 999);  // ignored — Apple already exists
        map.putIfAbsent("Cherry", 30);  // inserted — Cherry is new
        System.out.println("\nAfter putIfAbsent(Apple,999) and putIfAbsent(Cherry,30): " + map);

        // Three ways to iterate — entrySet is most efficient
        System.out.println("\nIteration via entrySet (most efficient):");
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            System.out.printf("  %-8s → %d%n", entry.getKey(), entry.getValue());
        }

        System.out.println("Iteration via forEach (Java 8+):");
        map.forEach((k, v) -> System.out.printf("  %-8s → %d%n", k, v));

        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 2 — HashMap Internals
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * HASHMAP INTERNAL STRUCTURE
     * ---------------------------
     * The backing store is declared as:
     *   transient Node<K,V>[] table;
     *
     * Each Node<K,V>:
     *   final int    hash;   ← cached hashCode (avoids recomputation)
     *   final K      key;
     *   V            value;
     *   Node<K,V>    next;   ← next node in the same bucket (chaining)
     *
     * STEP-BY-STEP: how put("Apple", 10) works
     * ------------------------------------------
     * 1. Compute raw hash:
     *      h = "Apple".hashCode()              → e.g. 63476538
     * 2. Spread the hash (perturbation):
     *      hash = h ^ (h >>> 16)               → mixes high bits into low bits
     *      This reduces collisions for keys with similar low-order bits.
     * 3. Compute bucket index:
     *      index = (table.length - 1) & hash   → e.g. index 6
     * 4. Is table[6] empty?
     *      YES → create Node, place at table[6]
     *      NO  → walk the linked list at table[6]:
     *              For each node: does node.hash == hash && node.key.equals("Apple")?
     *                YES → update node.value (key already present)
     *                NO  → continue chain
     *            End of chain → append new Node
     * 5. After insertion: if size > capacity * loadFactor → resize
     *
     * JAVA 8+ TREEIFICATION:
     *   When bucket chain length >= TREEIFY_THRESHOLD (8)
     *   AND table.length >= MIN_TREEIFY_CAPACITY (64)
     *   → convert linked list to Red-Black Tree
     *   → per-bucket worst case: O(n) → O(log n)
     *
     * UNTREEIFICATION:
     *   When tree size drops to UNTREEIFY_THRESHOLD (6) after removes
     *   → convert back to linked list
     */
    static void section2_HashMapInternals() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 2: HashMap Internals");
        System.out.println("─────────────────────────────────────────");

        Map<String, Integer> map = new HashMap<>();
        String[] keys = {"Apple", "Banana", "Cherry", "Apple", "Date"};

        System.out.println("Inserting keys — observing put() return values:");
        for (String k : keys) {
            int value = k.length(); // value = length of key for demo
            Integer old = map.put(k, value);
            System.out.printf("  put(%-8s, %2d) → old=%s  (hashCode=%d)%n",
                "\""+k+"\"", value, old, k.hashCode());
        }

        System.out.println("\nFinal map: " + map);
        System.out.println("Size: " + map.size() + " (5 puts, 1 update, 4 unique keys)");

        // Demonstrate bucket index formula
        System.out.println("\nBucket index simulation (capacity=16):");
        for (String k : new String[]{"Apple","Banana","Cherry","Date"}) {
            int h = k.hashCode();
            int spread = h ^ (h >>> 16);           // HashMap's internal spread
            int bucket = (16 - 1) & spread;
            System.out.printf("  %-8s raw=%10d spread=%10d bucket=%2d%n",
                k, h, spread, bucket);
        }

        // Null key — HashMap allows exactly one
        map.put(null, 0);
        System.out.println("\nnull key allowed in HashMap: map.get(null) = " + map.get(null));
        System.out.println("null key is stored in bucket 0 (hash treated as 0)");
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 3 — HashMap Resizing
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * RESIZE OPERATION
     * ----------------
     * Triggered when: size > capacity * loadFactor
     *
     * Default:
     *   capacity    = 16
     *   loadFactor  = 0.75
     *   threshold   = 16 * 0.75 = 12
     *
     * Resize doubles capacity: 16 → 32 → 64 → 128 → ...
     *
     * Steps:
     *   1. Allocate new array of size 2 * oldCapacity
     *   2. For each existing node: recompute bucket index
     *      (index is either same OR old_index + old_capacity)
     *   3. Place in new array
     *   4. Discard old array
     *
     * Why is new index either same or (old + oldCapacity)?
     *   Because capacity doubles, one new bit is added to the mask.
     *   If that bit in the hash is 0 → same index.
     *   If that bit is 1 → new index = old index + old capacity.
     *   This means half the nodes stay put and half move — O(n/2) on avg.
     *
     * Cost: O(n) per resize, but amortized O(1) per put() across all inserts.
     *
     * OPTIMIZATION: if you know the expected size upfront:
     *   new HashMap<>(expectedSize / 0.75 + 1)
     *   → avoids all resizing entirely
     */
    static void section3_HashMapResizing() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 3: HashMap Resizing");
        System.out.println("─────────────────────────────────────────");

        // Simulate resize threshold progression
        System.out.println("Default resize schedule (loadFactor=0.75):");
        int capacity = 16;
        for (int i = 0; i < 7; i++) {
            int threshold = (int)(capacity * 0.75);
            System.out.printf("  capacity=%6d  resize when size > %d%n", capacity, threshold);
            capacity *= 2;
        }

        // Pre-sized HashMap avoids all resizes for known workloads
        int expectedEntries = 1000;
        int preSizedCapacity = (int)(expectedEntries / 0.75) + 1; // = 1334
        Map<String, Integer> preSized = new HashMap<>(preSizedCapacity);
        System.out.printf("%nPre-sized for %,d entries: new HashMap<>(%d)%n",
            expectedEntries, preSizedCapacity);
        System.out.println("→ No resize will occur for up to 1000 entries\n");

        // Benchmark: default vs pre-sized
        int n = 200_000;
        long start = System.nanoTime();
        Map<Integer,Integer> defaultMap = new HashMap<>();
        for (int i = 0; i < n; i++) defaultMap.put(i, i);
        long defaultTime = System.nanoTime() - start;

        start = System.nanoTime();
        Map<Integer,Integer> preMap = new HashMap<>((int)(n / 0.75) + 1);
        for (int i = 0; i < n; i++) preMap.put(i, i);
        long preTime = System.nanoTime() - start;

        System.out.printf("Insert %,d entries:%n", n);
        System.out.printf("  Default HashMap (multiple resizes): %,d ns%n", defaultTime);
        System.out.printf("  Pre-sized HashMap (no resize):      %,d ns%n", preTime);
        System.out.printf("  Pre-sizing %.1fx faster%n%n",
            (double) defaultTime / preTime);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 4 — HashMap Java 8 Compute Methods
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * JAVA 8 MAP COMPUTE METHODS
     * ---------------------------
     * These are atomic read-modify-write operations on a map entry.
     * They replace the common pattern of:
     *   V old = map.get(key);
     *   if (old == null) ... else ...
     *   map.put(key, newValue);
     *
     * compute(K, BiFunction<K, V, V>)
     *   → always invoked, even if key absent (oldValue will be null)
     *   → if function returns null, entry is removed
     *
     * computeIfAbsent(K, Function<K, V>)
     *   → only invoked if key is absent or has null value
     *   → if function returns null, no entry is created
     *   → useful for initializing default values (e.g., new ArrayList<>())
     *
     * computeIfPresent(K, BiFunction<K, V, V>)
     *   → only invoked if key exists with non-null value
     *   → if function returns null, entry is removed
     *
     * merge(K, V, BiFunction<V, V, V>)
     *   → if key absent → put(key, value)
     *   → if key present → put(key, function(oldValue, newValue))
     *   → if function returns null → remove(key)
     *   → cleanest approach for frequency counting
     *
     * replaceAll(BiFunction<K, V, V>)
     *   → transforms all values in place
     */
    static void section4_HashMapJava8Methods() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 4: Java 8 Compute Methods");
        System.out.println("─────────────────────────────────────────");

        // ── computeIfAbsent — initialize default value ──────────────────────
        // Classic pattern: grouping items into lists
        Map<String, List<String>> grouped = new HashMap<>();
        String[][] data = {
            {"fruits",  "Apple"},
            {"veggies", "Carrot"},
            {"fruits",  "Banana"},
            {"veggies", "Pea"},
            {"fruits",  "Mango"}
        };

        for (String[] pair : data) {
            // If key absent, create new ArrayList; then add to that list
            grouped.computeIfAbsent(pair[0], k -> new ArrayList<>()).add(pair[1]);
        }
        System.out.println("computeIfAbsent (grouping):");
        grouped.forEach((k, v) -> System.out.println("  " + k + " → " + v));

        // ── merge — cleanest frequency counting ─────────────────────────────
        String[] words = {"apple", "banana", "apple", "cherry", "banana", "apple"};
        Map<String, Integer> freq = new HashMap<>();
        for (String w : words) {
            freq.merge(w, 1, Integer::sum);
            // If absent: put(w, 1)
            // If present: put(w, oldValue + 1)
        }
        System.out.println("\nmerge (frequency count): " + new TreeMap<>(freq));

        // ── compute — conditional update ─────────────────────────────────────
        Map<String, Integer> scores = new HashMap<>(Map.of("Alice", 80, "Bob", 90));
        // Double every score
        scores.replaceAll((name, score) -> score * 2);
        System.out.println("\nreplaceAll (double all scores): " + scores);

        // ── computeIfPresent — update only existing ──────────────────────────
        scores.computeIfPresent("Alice", (k, v) -> v + 10); // Alice gets +10 bonus
        scores.computeIfPresent("Ghost", (k, v) -> v + 10); // Ghost doesn't exist → no-op
        System.out.println("computeIfPresent (Alice +10, Ghost no-op): " + scores);

        // ── compute — remove if result is null ───────────────────────────────
        Map<String, Integer> stock = new HashMap<>(Map.of("Apple", 5, "Banana", 1));
        stock.compute("Banana", (k, v) -> (v == null || v <= 1) ? null : v - 1);
        System.out.println("\ncompute (remove Banana if stock ≤ 1): " + stock);

        // ── getOrDefault vs computeIfAbsent — important difference ───────────
        System.out.println("\ngetOrDefault vs computeIfAbsent:");
        Map<String, List<Integer>> m = new HashMap<>();
        List<Integer> defaultList = m.getOrDefault("key", new ArrayList<>());
        defaultList.add(42);
        System.out.println("  getOrDefault — map still empty: " + m.isEmpty()); // true!
        m.computeIfAbsent("key", k -> new ArrayList<>()).add(42);
        System.out.println("  computeIfAbsent — map has entry: " + !m.isEmpty() + " value=" + m.get("key"));
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 5 — TreeMap Basics
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * TREEMAP INTERNAL STRUCTURE
     * ---------------------------
     * Backed by: Red-Black Tree (same as TreeSet)
     * Each entry is an Entry<K,V> node in the tree:
     *
     *   static final class Entry<K,V> {
     *       K key;
     *       V value;
     *       Entry<K,V> left;
     *       Entry<K,V> right;
     *       Entry<K,V> parent;
     *       boolean color;   // RED or BLACK
     *   }
     *
     * Keys are always kept sorted by:
     *   - Natural ordering (Comparable.compareTo) if no Comparator given
     *   - Custom Comparator if provided in constructor
     *
     * TreeMap implements NavigableMap which extends SortedMap.
     * This gives access to powerful range and navigation operations.
     */
    static void section5_TreeMapBasics() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 5: TreeMap Basics");
        System.out.println("─────────────────────────────────────────");

        TreeMap<Integer, String> treeMap = new TreeMap<>();
        treeMap.put(30, "C");
        treeMap.put(10, "A");
        treeMap.put(50, "E");
        treeMap.put(20, "B");
        treeMap.put(40, "D");

        System.out.println("Inserted: 30, 10, 50, 20, 40");
        System.out.println("TreeMap (always sorted by key): " + treeMap);

        // Overwriting a value — returns old value
        String old = treeMap.put(20, "UPDATED");
        System.out.println("put(20, UPDATED) → old value: " + old);
        System.out.println("After update: " + treeMap);

        // firstKey / lastKey
        System.out.println("\nfirstKey(): " + treeMap.firstKey()); // 10
        System.out.println("lastKey():  " + treeMap.lastKey());   // 50

        // firstEntry / lastEntry — returns key AND value
        System.out.println("firstEntry(): " + treeMap.firstEntry()); // 10=A
        System.out.println("lastEntry():  " + treeMap.lastEntry());  // 50=E

        // Iteration in sorted key order
        System.out.println("\nIn-order iteration (sorted by key):");
        treeMap.forEach((k, v) -> System.out.printf("  %d → %s%n", k, v));
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 6 — TreeMap Navigation Methods
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * NAVIGABLEMAP METHODS
     * ---------------------
     * TreeMap implements NavigableMap, giving access to:
     *
     * floorKey(k)     → largest key ≤ k      (or null)
     * ceilingKey(k)   → smallest key ≥ k     (or null)
     * lowerKey(k)     → strictly < k         (or null)
     * higherKey(k)    → strictly > k         (or null)
     *
     * headMap(k)      → submap with keys strictly < k
     * tailMap(k)      → submap with keys ≥ k
     * subMap(from, to)→ submap with keys from ≤ k < to
     *
     * These are all views — backed by the same tree.
     * Modifications to the view modify the original map.
     *
     * pollFirstEntry() → retrieve AND remove minimum key entry
     * pollLastEntry()  → retrieve AND remove maximum key entry
     *
     * descendingMap()  → NavigableMap with keys in reverse order
     */
    static void section6_TreeMapNavigation() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 6: TreeMap Navigation Methods");
        System.out.println("─────────────────────────────────────────");

        TreeMap<Integer, String> prices = new TreeMap<>();
        prices.put(100, "Budget");
        prices.put(300, "Mid");
        prices.put(500, "Premium");
        prices.put(700, "Luxury");
        prices.put(1000, "Ultra");
        System.out.println("Price tiers: " + prices);

        // Real use: find the right pricing tier for a given budget
        int budget = 450;
        System.out.printf("%nFor budget $%d:%n", budget);
        System.out.println("  floorKey (≤ budget):   $" + prices.floorKey(budget)
                           + " → " + prices.floorEntry(budget).getValue());
        System.out.println("  ceilingKey (≥ budget): $" + prices.ceilingKey(budget)
                           + " → " + prices.ceilingEntry(budget).getValue());
        System.out.println("  lowerKey (< budget):   $" + prices.lowerKey(budget));
        System.out.println("  higherKey (> budget):  $" + prices.higherKey(budget));

        // Range views
        System.out.println("\nheadMap(500) — tiers below $500: " + prices.headMap(500));
        System.out.println("tailMap(500) — tiers $500+:      " + prices.tailMap(500));
        System.out.println("subMap(300,700) — $300 to <$700: " + prices.subMap(300, 700));

        // Descending view
        System.out.println("descendingMap():                 " + prices.descendingMap());

        // pollFirstEntry / pollLastEntry
        TreeMap<Integer, String> copy = new TreeMap<>(prices);
        System.out.println("\npollFirstEntry(): " + copy.pollFirstEntry() + " → remaining: " + copy);
        System.out.println("pollLastEntry():  " + copy.pollLastEntry()  + " → remaining: " + copy);
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 7 — TreeMap Custom Comparator
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * TREEMAP CUSTOM COMPARATOR
     * --------------------------
     * The Comparator is passed to the TreeMap constructor.
     * It governs BOTH the sort order AND equality:
     *   If comparator.compare(k1, k2) == 0 → they are the SAME key.
     *
     * This has the same BigDecimal-style trap as TreeSet.
     * Always ensure your Comparator is consistent with equals().
     */
    static void section7_TreeMapCustomComparator() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 7: TreeMap Custom Comparator");
        System.out.println("─────────────────────────────────────────");

        // Reverse order TreeMap
        TreeMap<String, Integer> reverseMap =
            new TreeMap<>(Comparator.reverseOrder());
        reverseMap.put("Banana", 2);
        reverseMap.put("Apple",  1);
        reverseMap.put("Mango",  3);
        reverseMap.put("Cherry", 4);
        System.out.println("Reverse alphabetical: " + reverseMap);

        // Sort by string length, then alphabetically
        TreeMap<String, Integer> byLength = new TreeMap<>(
            Comparator.comparingInt(String::length)
                      .thenComparing(Comparator.naturalOrder())
        );
        byLength.put("Kiwi",   1);
        byLength.put("Apple",  2);
        byLength.put("Fig",    3);
        byLength.put("Mango",  4);
        byLength.put("Plum",   5);
        byLength.put("Banana", 6);
        System.out.println("By length then alpha: " + byLength);

        // Case-insensitive keys — "Apple" and "apple" are same key
        TreeMap<String, Integer> caseInsensitive =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitive.put("Apple", 1);
        caseInsensitive.put("apple", 99); // overwrites "Apple" — same key by comparator
        caseInsensitive.put("BANANA", 2);
        System.out.println("Case-insensitive TreeMap: " + caseInsensitive
                           + " (size=" + caseInsensitive.size() + ")");

        // Integer keys sorted by absolute value
        TreeMap<Integer, String> absOrder =
            new TreeMap<>(Comparator.comparingInt(Math::abs));
        absOrder.put(-5, "neg5");
        absOrder.put( 2, "pos2");
        absOrder.put(-1, "neg1");
        absOrder.put( 4, "pos4");
        System.out.println("Sorted by absolute value: " + absOrder);
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 8 — LinkedHashMap
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * LINKEDHASHMAP
     * --------------
     * Extends HashMap and adds a doubly linked list through all entries.
     * Two ordering modes:
     *
     * MODE 1: Insertion order (default)
     *   new LinkedHashMap<>()
     *   Elements iterate in the order they were first inserted.
     *
     * MODE 2: Access order
     *   new LinkedHashMap<>(capacity, loadFactor, true)
     *   On every get() or put(), the accessed entry moves to the tail.
     *   Least-recently-used entry is always at the head.
     *   → This is how you build an LRU cache.
     *
     * removeEldestEntry() hook:
     *   Override this to automatically evict entries when the map grows
     *   beyond a threshold — a built-in LRU eviction mechanism.
     */
    static void section8_LinkedHashMap() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 8: LinkedHashMap — Insertion & LRU");
        System.out.println("─────────────────────────────────────────");

        // Mode 1 — insertion order (useful for preserving config order)
        Map<String, String> config = new LinkedHashMap<>();
        config.put("host",    "localhost");
        config.put("port",    "8080");
        config.put("timeout", "30s");
        config.put("retries", "3");
        System.out.println("Config (insertion order preserved):");
        config.forEach((k, v) -> System.out.println("  " + k + "=" + v));

        // Compare with HashMap — order lost
        Map<String, String> hashConfig = new HashMap<>(config);
        System.out.println("Same config in HashMap: " + hashConfig.keySet()
                           + " ← order not preserved");

        // Mode 2 — LRU cache using access-order LinkedHashMap
        final int LRU_CAPACITY = 3;
        Map<Integer, String> lruCache = new LinkedHashMap<>(
            LRU_CAPACITY, 0.75f, true  // ← true = access order
        ) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, String> eldest) {
                boolean evict = size() > LRU_CAPACITY;
                if (evict) System.out.println("  ♻ Evicting: " + eldest.getKey()
                                             + "=" + eldest.getValue());
                return evict;
            }
        };

        System.out.println("\nLRU Cache (capacity=" + LRU_CAPACITY + "):");
        lruCache.put(1, "Page A"); System.out.println("put(1): " + lruCache.keySet());
        lruCache.put(2, "Page B"); System.out.println("put(2): " + lruCache.keySet());
        lruCache.put(3, "Page C"); System.out.println("put(3): " + lruCache.keySet());
        lruCache.get(1);           System.out.println("get(1): " + lruCache.keySet()
                                                       + " ← 1 moved to tail (most recent)");
        lruCache.put(4, "Page D"); System.out.println("put(4): " + lruCache.keySet()
                                                       + " ← 2 evicted (least recent)");
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 9 — ConcurrentHashMap
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * WHY NOT HASHMAP IN MULTI-THREADED CODE?
     * ----------------------------------------
     * HashMap is NOT thread-safe. Concurrent modifications cause:
     *   - Lost updates (two threads both read old value, both write new)
     *   - Infinite loops (in Java 7, resize could create circular list)
     *   - ConcurrentModificationException
     *
     * WHY NOT HASHTABLE OR COLLECTIONS.SYNCHRONIZEDMAP?
     * ---------------------------------------------------
     * Both synchronize on the ENTIRE map for every operation.
     * Only one thread can read OR write at any time.
     * Throughput degrades to single-threaded at high concurrency.
     *
     * WHY CONCURRENTHASHMAP?
     * -----------------------
     * Java 7:  Segment-level locking (16 segments by default)
     *          Up to 16 threads can write concurrently.
     *
     * Java 8+: CAS (Compare-And-Swap) + bucket-level locking
     *          - Empty bucket: use CAS — no lock at all
     *          - Non-empty bucket: lock only that bucket's head node
     *          - Reads: completely lock-free
     *
     * This means in practice:
     *   - Reads:  zero contention — always parallel
     *   - Writes: contention only on the exact same bucket
     *             (probability ≈ 1/n for n buckets)
     *
     * NULL PROHIBITION:
     *   ConcurrentHashMap does NOT allow null keys or null values.
     *   Reason: get(key) returning null is ambiguous — does the key not
     *   exist, or does it map to null? In a concurrent context you cannot
     *   safely do a followup containsKey() because the state may change.
     */
    static void section9_ConcurrentHashMap() throws InterruptedException {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 9: ConcurrentHashMap");
        System.out.println("─────────────────────────────────────────");

        // Basic operations — same API as HashMap
        ConcurrentHashMap<String, Integer> chm = new ConcurrentHashMap<>();
        chm.put("A", 1);
        chm.put("B", 2);
        chm.put("C", 3);
        System.out.println("ConcurrentHashMap: " + chm);

        // Null prohibition
        try {
            chm.put(null, 0);
        } catch (NullPointerException e) {
            System.out.println("put(null, 0) → NullPointerException ← null keys not allowed");
        }
        try {
            chm.put("D", null);
        } catch (NullPointerException e) {
            System.out.println("put(D, null) → NullPointerException ← null values not allowed");
        }

        // Thread-safe concurrent increment — the RIGHT way
        // putIfAbsent + compute pattern
        ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

        Runnable incrementTask = () -> {
            for (int i = 0; i < 1000; i++) {
                counters.computeIfAbsent("hits", k -> new AtomicInteger(0))
                        .incrementAndGet();
            }
        };

        Thread t1 = new Thread(incrementTask);
        Thread t2 = new Thread(incrementTask);
        Thread t3 = new Thread(incrementTask);
        t1.start(); t2.start(); t3.start();
        t1.join();  t2.join();  t3.join();

        System.out.println("\n3 threads each incrementing 1000 times:");
        System.out.println("Expected: 3000, Actual: " + counters.get("hits").get());

        // Atomic operations on ConcurrentHashMap
        ConcurrentHashMap<String, Integer> scores = new ConcurrentHashMap<>();
        scores.put("Alice", 100);
        System.out.println("\nAtomic operations:");
        scores.merge("Alice", 50, Integer::sum);
        System.out.println("  merge(Alice, 50, sum): " + scores.get("Alice")); // 150
        scores.merge("Bob",   50, Integer::sum);
        System.out.println("  merge(Bob,   50, sum): " + scores.get("Bob"));   // 50 (new)

        System.out.println("\nDifference from HashMap.merge: ConcurrentHashMap.merge is atomic");
        System.out.println("→ No race condition between read + compute + write");
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 10 — ConcurrentHashMap Advanced Internals
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * CAS — COMPARE AND SWAP
     * -----------------------
     * CAS is a CPU instruction that atomically:
     *   1. Reads current value at memory address
     *   2. Compares it to an expected value
     *   3. If match: writes new value and returns true
     *   4. If no match: does nothing and returns false
     *
     * Pseudocode:
     *   boolean CAS(address, expectedValue, newValue) {
     *       if (*address == expectedValue) { *address = newValue; return true; }
     *       else return false;
     *   }
     *
     * ConcurrentHashMap uses CAS for empty bucket insertions:
     *   if (CAS(table[index], null, newNode)) → success, no lock needed
     *   else → bucket now has a node, fall back to synchronized block
     *
     * BULK OPERATIONS (Java 8+):
     *   forEach(parallelismThreshold, action)
     *   reduce(parallelismThreshold, transformer, reducer)
     *   search(parallelismThreshold, searchFunction)
     *
     *   If map.size() < parallelismThreshold → sequential
     *   If map.size() ≥ parallelismThreshold → parallel (uses ForkJoinPool)
     *
     * SIZE COUNTING:
     *   size() is approximate under concurrency — uses a distributed counter
     *   (similar to LongAdder) to avoid contention on a single counter.
     *   mappingCount() returns a long — prefer this over size() for large maps.
     */
    static void section10_ConcurrentHashMapInternals() throws InterruptedException {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 10: ConcurrentHashMap Internals");
        System.out.println("─────────────────────────────────────────");

        // Demonstrate thread safety vs HashMap
        int numThreads = 5, insertsPerThread = 10_000;

        // HashMap — NOT thread safe (may lose entries)
        Map<Integer, Integer> unsafeMap = new HashMap<>();
        List<Thread> threads = new ArrayList<>();
        AtomicInteger idGen = new AtomicInteger(0);

        for (int t = 0; t < numThreads; t++) {
            threads.add(new Thread(() -> {
                for (int i = 0; i < insertsPerThread; i++) {
                    int id = idGen.getAndIncrement();
                    unsafeMap.put(id, id); // race condition!
                }
            }));
        }
        threads.forEach(Thread::start);
        for (Thread t : threads) t.join();

        // ConcurrentHashMap — thread safe (exact count guaranteed)
        ConcurrentHashMap<Integer, Integer> safeMap = new ConcurrentHashMap<>();
        threads.clear();
        idGen.set(0);

        for (int t = 0; t < numThreads; t++) {
            threads.add(new Thread(() -> {
                for (int i = 0; i < insertsPerThread; i++) {
                    int id = idGen.getAndIncrement();
                    safeMap.put(id, id);
                }
            }));
        }
        threads.forEach(Thread::start);
        for (Thread t : threads) t.join();

        int expected = numThreads * insertsPerThread;
        System.out.printf("%d threads × %,d inserts each = %,d expected%n",
            numThreads, insertsPerThread, expected);
        System.out.printf("HashMap size (unsafe):          %,d  %s%n",
            unsafeMap.size(), unsafeMap.size() == expected ? "✅" : "❌ LOST ENTRIES");
        System.out.printf("ConcurrentHashMap size (safe):  %,d  %s%n",
            safeMap.size(), safeMap.size() == expected ? "✅" : "❌");

        // Bulk operations
        System.out.println("\nBulk forEach (parallel when size ≥ threshold):");
        ConcurrentHashMap<String, Integer> bulk = new ConcurrentHashMap<>();
        bulk.put("A", 1); bulk.put("B", 2); bulk.put("C", 3); bulk.put("D", 4);

        // parallelismThreshold=1 → always parallel; Long.MAX_VALUE → always sequential
        long sum = bulk.reduceValues(1, Integer::sum);
        System.out.println("  reduceValues(sum): " + sum);
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 11 — Frequency Counting
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * FREQUENCY COUNTING — MULTIPLE APPROACHES
     * ------------------------------------------
     * All approaches below are O(n) time.
     *
     * getOrDefault:    readable, works in all Java versions
     * merge:           most concise, Java 8+
     * compute:         more explicit control
     * computeIfAbsent: useful when value type needs initialization
     * Collectors.groupingBy + counting: one-liner streams
     */
    static void section11_FrequencyCounting() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 11: Frequency Counting");
        System.out.println("─────────────────────────────────────────");

        String[] words = {"apple", "banana", "apple", "cherry",
                          "banana", "apple", "date", "cherry"};
        System.out.println("Input: " + Arrays.toString(words));

        // Approach 1 — getOrDefault (most readable, pre-Java 8 safe)
        Map<String, Integer> freq1 = new HashMap<>();
        for (String w : words) freq1.put(w, freq1.getOrDefault(w, 0) + 1);
        System.out.println("\nApproach 1 — getOrDefault: " + new TreeMap<>(freq1));

        // Approach 2 — merge (most concise)
        Map<String, Integer> freq2 = new HashMap<>();
        for (String w : words) freq2.merge(w, 1, Integer::sum);
        System.out.println("Approach 2 — merge:         " + new TreeMap<>(freq2));

        // Approach 3 — compute
        Map<String, Integer> freq3 = new HashMap<>();
        for (String w : words) freq3.compute(w, (k, v) -> v == null ? 1 : v + 1);
        System.out.println("Approach 3 — compute:       " + new TreeMap<>(freq3));

        // Approach 4 — streams
        Map<String, Long> freq4 = Arrays.stream(words)
            .collect(Collectors.groupingBy(w -> w, Collectors.counting()));
        System.out.println("Approach 4 — streams:       " + new TreeMap<>(freq4));

        // Sort by frequency descending
        System.out.println("\nSorted by frequency (desc):");
        freq1.entrySet().stream()
             .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
             .forEach(e -> System.out.printf("  %-8s → %d%n", e.getKey(), e.getValue()));

        // Top N frequent words
        int N = 2;
        System.out.println("\nTop " + N + " frequent words:");
        freq1.entrySet().stream()
             .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
             .limit(N)
             .forEach(e -> System.out.printf("  %-8s → %d%n", e.getKey(), e.getValue()));
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 12 — Real World: Session Cache
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * REAL WORLD — SESSION CACHING
     * ------------------------------
     * A session cache maps session IDs to user data.
     *
     * Requirements:
     *   - O(1) lookup by session ID
     *   - No ordering needed
     *   - Thread-safe if multiple threads serve requests
     *
     * Single-threaded → HashMap
     * Multi-threaded  → ConcurrentHashMap
     */
    static void section12_RealWorldCaching() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 12: Real World — Session Cache");
        System.out.println("─────────────────────────────────────────");

        record UserSession(String userId, String role, long expiresAt) {}

        ConcurrentHashMap<String, UserSession> sessionCache = new ConcurrentHashMap<>();

        // Simulating session creation
        long now = System.currentTimeMillis();
        sessionCache.put("sess-001", new UserSession("alice", "admin",  now + 3600_000));
        sessionCache.put("sess-002", new UserSession("bob",   "viewer", now + 1800_000));
        sessionCache.put("sess-003", new UserSession("carol", "editor", now + 3600_000));

        System.out.println("Active sessions: " + sessionCache.size());

        // O(1) session lookup
        String incomingToken = "sess-002";
        UserSession session = sessionCache.get(incomingToken);
        if (session != null && session.expiresAt() > System.currentTimeMillis()) {
            System.out.println("Authenticated: " + session.userId()
                               + " (role=" + session.role() + ")");
        }

        // Invalidate session on logout
        sessionCache.remove("sess-001");
        System.out.println("After logout: " + sessionCache.size() + " sessions active");

        // Expire sessions — computeIfPresent approach
        sessionCache.entrySet().removeIf(
            e -> e.getValue().expiresAt() < System.currentTimeMillis()
        );
        System.out.println("After expiry cleanup: " + sessionCache.size() + " sessions");
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 13 — Real World: Grouping and Partitioning
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * REAL WORLD — GROUPING DATA
     * ---------------------------
     * A very common pattern: group a flat list into a Map of lists.
     *   Map<Department, List<Employee>>
     *   Map<OrderStatus, List<Order>>
     *   Map<Country, List<Customer>>
     *
     * Options:
     *   computeIfAbsent + add  → most explicit, good for complex mutations
     *   merge                  → useful for aggregating primitives
     *   Collectors.groupingBy  → cleanest for Stream pipelines
     */
    static void section13_RealWorldGrouping() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 13: Real World — Grouping Data");
        System.out.println("─────────────────────────────────────────");

        record Employee(String name, String dept, int salary) {}

        List<Employee> employees = List.of(
            new Employee("Alice", "Engineering", 120_000),
            new Employee("Bob",   "Marketing",    80_000),
            new Employee("Carol", "Engineering", 130_000),
            new Employee("Dave",  "HR",           70_000),
            new Employee("Eve",   "Marketing",    90_000),
            new Employee("Frank", "Engineering", 110_000)
        );

        // Group by department — computeIfAbsent approach
        Map<String, List<Employee>> byDept = new HashMap<>();
        for (Employee e : employees) {
            byDept.computeIfAbsent(e.dept(), k -> new ArrayList<>()).add(e);
        }
        System.out.println("Employees by department:");
        new TreeMap<>(byDept).forEach((dept, list) -> {
            System.out.println("  " + dept + ":");
            list.forEach(e -> System.out.printf("    %-8s $%,d%n", e.name(), e.salary()));
        });

        // Average salary by department — merge / streams
        Map<String, Double> avgSalary = employees.stream()
            .collect(Collectors.groupingBy(
                Employee::dept,
                Collectors.averagingInt(Employee::salary)
            ));
        System.out.println("\nAverage salary by department:");
        new TreeMap<>(avgSalary).forEach((dept, avg) ->
            System.out.printf("  %-14s $%,.0f%n", dept, avg));

        // Total salary by department
        Map<String, Integer> totalSalary = new HashMap<>();
        for (Employee e : employees) totalSalary.merge(e.dept(), e.salary(), Integer::sum);
        System.out.println("\nTotal salary by department:");
        new TreeMap<>(totalSalary).forEach((dept, total) ->
            System.out.printf("  %-14s $%,d%n", dept, total));

        // Partition into two groups: salary >= 100k or not
        Map<Boolean, List<Employee>> partitioned = employees.stream()
            .collect(Collectors.partitioningBy(e -> e.salary() >= 100_000));
        System.out.println("\nSalary ≥ $100k: "
            + partitioned.get(true).stream().map(Employee::name).collect(Collectors.toList()));
        System.out.println("Salary < $100k: "
            + partitioned.get(false).stream().map(Employee::name).collect(Collectors.toList()));
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 14 — Real World: Anagram Detection
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * REAL WORLD — ANAGRAM DETECTION AND GROUPING
     * ---------------------------------------------
     * Classic interview problem: group a list of words by their anagram signature.
     * Two words are anagrams if they contain the same characters with the same frequency.
     *
     * Signature: sort the characters of each word → use as map key.
     *   "eat" → "aet"
     *   "tea" → "aet"  same key → same group
     *   "tan" → "ant"
     *   "nat" → "ant"  same key → same group
     *
     * Time: O(n * k log k) where n = word count, k = max word length
     * Space: O(n * k)
     */
    static void section14_RealWorldAnagram() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 14: Real World — Anagram Grouping");
        System.out.println("─────────────────────────────────────────");

        String[] words = {"eat", "tea", "tan", "ate", "nat", "bat",
                          "listen", "silent", "enlist", "google", "goolog"};

        // Group by sorted-character signature
        Map<String, List<String>> anagramGroups = new HashMap<>();
        for (String word : words) {
            char[] chars = word.toCharArray();
            Arrays.sort(chars);
            String signature = new String(chars);
            anagramGroups.computeIfAbsent(signature, k -> new ArrayList<>()).add(word);
        }

        System.out.println("Anagram groups:");
        anagramGroups.values().stream()
            .sorted(Comparator.comparingInt(List::size).reversed())
            .forEach(group -> System.out.println("  " + group));

        // Count distinct anagram groups
        long groupsWithMultiple = anagramGroups.values().stream()
            .filter(g -> g.size() > 1).count();
        System.out.println("Groups with multiple anagrams: " + groupsWithMultiple);

        // Map-based two-sum problem (classic interview use of HashMap)
        System.out.println("\nTwo-Sum problem (target=9):");
        int[] nums = {2, 7, 11, 15, 1, 8};
        int target = 9;
        Map<Integer, Integer> seen = new HashMap<>(); // value → index
        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];
            if (seen.containsKey(complement)) {
                System.out.printf("  Found: nums[%d]=%d + nums[%d]=%d = %d%n",
                    seen.get(complement), complement, i, nums[i], target);
            }
            seen.put(nums[i], i);
        }
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 15 — Common Mistakes
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * COMMON MISTAKES WITH MAP
     * -------------------------
     *
     * 1. Mutable keys — key's hashCode changes after insertion
     *    → entry becomes unreachable in wrong bucket
     *
     * 2. Not overriding equals()/hashCode() on custom key objects
     *    → logically equal keys treated as different → duplicates
     *
     * 3. Expecting order from HashMap
     *    → Use LinkedHashMap (insertion) or TreeMap (sorted)
     *
     * 4. Using HashMap in multi-threaded code
     *    → Use ConcurrentHashMap
     *
     * 5. HashMap vs ConcurrentHashMap null handling
     *    → CHM does not allow null key or null value
     *
     * 6. getOrDefault does NOT insert the default into the map
     *    → Use computeIfAbsent if you need the default stored
     *
     * 7. Modifying map while iterating via entrySet/keySet
     *    → ConcurrentModificationException (HashMap)
     *    → Use iterator.remove() or removeIf() or ConcurrentHashMap
     *
     * 8. containsValue is O(n) — not O(1)
     *    → If you need fast value lookup, maintain an inverse map
     */
    static void section15_CommonMistakes() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 15: Common Mistakes");
        System.out.println("─────────────────────────────────────────");

        // Mistake 1 — mutable key
        System.out.println("Mistake 1: Mutable key causes lost entry");
        Map<List<Integer>, String> mutableKeyMap = new HashMap<>();
        List<Integer> key = new ArrayList<>(List.of(1, 2, 3));
        mutableKeyMap.put(key, "original");
        System.out.println("  Before mutation: contains key? " + mutableKeyMap.containsKey(key));
        key.add(4); // mutate the key — its hashCode changes
        System.out.println("  After mutation:  contains key? " + mutableKeyMap.containsKey(key)
                           + " ← entry lost in wrong bucket!");
        System.out.println("  ✅ Always use immutable keys (String, Integer, record, etc.)");

        // Mistake 2 — ConcurrentModificationException
        System.out.println("\nMistake 2: Modifying map while iterating");
        Map<String, Integer> map = new HashMap<>(Map.of("a", 1, "b", 2, "c", 3));
        try {
            for (String k : map.keySet()) {
                if (map.get(k) < 2) map.remove(k); // ❌ modifies during iteration
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("  ❌ ConcurrentModificationException");
        }
        // Fix: use removeIf on entrySet
        Map<String, Integer> map2 = new HashMap<>(Map.of("a", 1, "b", 2, "c", 3));
        map2.entrySet().removeIf(e -> e.getValue() < 2); // ✅ safe
        System.out.println("  ✅ removeIf on entrySet: " + map2);

        // Mistake 3 — getOrDefault does not store the default
        System.out.println("\nMistake 3: getOrDefault does NOT insert the default");
        Map<String, List<String>> m = new HashMap<>();
        List<String> list = m.getOrDefault("key", new ArrayList<>());
        list.add("item"); // adds to the local list — NOT stored in map
        System.out.println("  map after getOrDefault + add: " + m.isEmpty() + " empty ← item lost!");
        m.computeIfAbsent("key", k -> new ArrayList<>()).add("item"); // ✅
        System.out.println("  map after computeIfAbsent + add: " + m);

        // Mistake 4 — Map.of() is immutable
        System.out.println("\nMistake 4: Map.of() is immutable");
        Map<String, Integer> immutable = Map.of("a", 1, "b", 2);
        try {
            immutable.put("c", 3);
        } catch (UnsupportedOperationException e) {
            System.out.println("  ❌ Map.of().put() → UnsupportedOperationException");
            System.out.println("  ✅ Use new HashMap<>(Map.of(...)) for mutable copy");
        }

        // Mistake 5 — ConcurrentHashMap null
        System.out.println("\nMistake 5: ConcurrentHashMap rejects null key/value");
        ConcurrentHashMap<String, String> chm = new ConcurrentHashMap<>();
        try { chm.put(null, "v"); } catch (NullPointerException e) {
            System.out.println("  ❌ CHM.put(null, v) → NullPointerException");
        }
        try { chm.put("k", null); } catch (NullPointerException e) {
            System.out.println("  ❌ CHM.put(k, null) → NullPointerException");
        }
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 16 — Performance Benchmark
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * PERFORMANCE COMPARISON
     * -----------------------
     * HashMap:
     *   put / get / remove → O(1) average
     *   n inserts total    → O(n)
     *
     * TreeMap:
     *   put / get / remove → O(log n) guaranteed
     *   n inserts total    → O(n log n)
     *
     * LinkedHashMap:
     *   put / get / remove → O(1) average (same as HashMap + linked list pointer)
     *
     * ConcurrentHashMap (single-threaded):
     *   Slightly slower than HashMap due to volatile reads + CAS overhead
     *
     * ConcurrentHashMap (multi-threaded):
     *   Scales nearly linearly with thread count for writes to different buckets
     *   HashMap would corrupt under same load
     */
    static void section16_PerformanceBenchmark() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 16: Performance Benchmark");
        System.out.println("─────────────────────────────────────────");

        int n = 500_000;
        Random rng = new Random(42);
        int[] keys = rng.ints(n, 0, n).toArray();

        // Insert benchmark
        long start;
        Map<Integer,Integer> hashMap    = new HashMap<>();
        Map<Integer,Integer> treeMap    = new TreeMap<>();
        Map<Integer,Integer> linkedMap  = new LinkedHashMap<>();
        Map<Integer,Integer> concMap    = new ConcurrentHashMap<>();

        start = System.nanoTime();
        for (int k : keys) hashMap.put(k, k);
        long hashInsert = System.nanoTime() - start;

        start = System.nanoTime();
        for (int k : keys) treeMap.put(k, k);
        long treeInsert = System.nanoTime() - start;

        start = System.nanoTime();
        for (int k : keys) linkedMap.put(k, k);
        long linkedInsert = System.nanoTime() - start;

        start = System.nanoTime();
        for (int k : keys) concMap.put(k, k);
        long concInsert = System.nanoTime() - start;

        System.out.printf("Insert %,d entries:%n", n);
        System.out.printf("  HashMap:            %,8d ns%n", hashInsert);
        System.out.printf("  LinkedHashMap:      %,8d ns%n", linkedInsert);
        System.out.printf("  ConcurrentHashMap:  %,8d ns%n", concInsert);
        System.out.printf("  TreeMap:            %,8d ns  (%.1fx slower — O(n log n))%n",
            treeInsert, (double) treeInsert / hashInsert);

        // Lookup benchmark
        int[] lookupKeys = rng.ints(100_000, 0, n).toArray();

        start = System.nanoTime();
        int h = 0; for (int k : lookupKeys) if (hashMap.containsKey(k)) h++;
        long hashLookup = System.nanoTime() - start;

        start = System.nanoTime();
        int t = 0; for (int k : lookupKeys) if (treeMap.containsKey(k)) t++;
        long treeLookup = System.nanoTime() - start;

        System.out.printf("%n100,000 get() lookups:%n");
        System.out.printf("  HashMap O(1):   %,8d ns  hits=%,d%n", hashLookup, h);
        System.out.printf("  TreeMap O(logn):%,8d ns  hits=%,d%n", treeLookup, t);
        System.out.printf("  TreeMap %.1fx slower for lookups%n%n",
            (double) treeLookup / hashLookup);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 17 — Interview Summary
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * INTERVIEW ANSWERS
     * ------------------
     *
     * Q: Why is HashMap average O(1)?
     * A: hashCode() maps the key to a bucket in O(1). If the bucket contains
     *    0–1 elements (the common case with a good hash function), the
     *    operation completes in O(1). Amortized O(1) across puts because
     *    resizing is rare and its cost spreads across all prior inserts.
     *
     * Q: Why worst case O(n) (or O(log n) in Java 8+)?
     * A: If all keys hash to the same bucket (e.g., adversarial hash function
     *    or all keys return the same hashCode()), every operation traverses
     *    the entire chain. Java 8+ treeifies buckets ≥ 8 entries, improving
     *    per-bucket worst case to O(log n).
     *
     * Q: Why did Java 8 introduce tree bins?
     * A: To defend against HashDoS attacks where an attacker crafts keys
     *    with the same hashCode, degrading HashMap to O(n) per operation
     *    and causing denial of service. Tree bins cap per-bucket cost at O(log n).
     *
     * Q: When to use ConcurrentHashMap vs Collections.synchronizedMap?
     * A: Always prefer ConcurrentHashMap for concurrent access.
     *    synchronizedMap locks the entire map for every read AND write.
     *    ConcurrentHashMap uses CAS + bucket-level locking for writes,
     *    and reads are completely lock-free. Under high concurrency,
     *    ConcurrentHashMap can be orders of magnitude faster.
     *
     * Q: Difference between synchronizedMap and ConcurrentHashMap?
     *
     *    synchronizedMap:
     *      - Wraps any Map with synchronized(mutex) on every method
     *      - Full map locked for every read and write
     *      - Iteration must be manually synchronized
     *      - Allows null keys/values
     *
     *    ConcurrentHashMap:
     *      - Lock-free reads, bucket-level locked writes (Java 8+)
     *      - Size is approximate under concurrency
     *      - Iterator is weakly consistent (won't throw CME)
     *      - No null keys or values
     *
     * Q: When to use which Map?
     * A: See the golden rules below.
     */
    static void section17_InterviewSummary() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 17: Interview Cheat Sheet");
        System.out.println("─────────────────────────────────────────");

        System.out.println("""
                HASHMAP
                  Internal:    Node<K,V>[] table (hash table with chaining)
                  put/get:     O(1) avg, O(log n) worst (Java 8+ treeified bucket)
                  Order:       None guaranteed
                  Null:        One null key, null values allowed
                  Thread-safe: ❌ No
                  Best for:    General-purpose key-value store, fastest option
                  Requires:    hashCode() + equals() on key class

                TREEMAP
                  Internal:    Red-Black Tree (Entry<K,V> nodes)
                  put/get:     O(log n) guaranteed
                  Order:       Always sorted by key (natural or Comparator)
                  Null:        No null keys, null values allowed
                  Thread-safe: ❌ No
                  Best for:    Sorted data, range queries, ordered reports
                  Extras:      floor, ceiling, headMap, tailMap, subMap

                LINKEDHASHMAP
                  Internal:    HashMap + doubly linked list
                  put/get:     O(1) avg
                  Order:       Insertion order (or access order for LRU)
                  Null:        One null key, null values allowed
                  Thread-safe: ❌ No
                  Best for:    Ordered iteration, LRU cache, config maps

                CONCURRENTHASHMAP
                  Internal:    Hash table, CAS + bucket-level locking (Java 8+)
                  put/get:     O(1) avg
                  Order:       None guaranteed
                  Null:        ❌ No null keys or values
                  Thread-safe: ✅ Yes (fine-grained)
                  Best for:    Multi-threaded caches, counters, shared state
                  size():      Approximate under concurrency — use mappingCount()

                GOLDEN RULES
                  1.  Default to HashMap — fastest for single-threaded use
                  2.  Use TreeMap only when sorted keys or range queries needed
                  3.  Use LinkedHashMap for insertion-order iteration or LRU cache
                  4.  Use ConcurrentHashMap for any multi-threaded access
                  5.  Never use Hashtable in new code — ConcurrentHashMap replaces it
                  6.  Always override hashCode() AND equals() together on key objects
                  7.  Use immutable key objects (String, Integer, records)
                  8.  Pre-size HashMap: new HashMap<>(expectedSize / 0.75 + 1)
                  9.  getOrDefault does NOT insert — use computeIfAbsent to store
                  10. Map.of() / Map.copyOf() return immutable maps
                  11. Never null-check the result of ConcurrentHashMap.get() for absence
                      — CHM never returns null, so null means key absent (unambiguous)
                  12. entrySet() iteration is more efficient than keySet() + get()
                """);

        System.out.println("Scenario decision table:");
        System.out.println("  10M reads, no order, single-threaded  → HashMap        O(1)");
        System.out.println("  Sorted reporting system               → TreeMap         O(log n)");
        System.out.println("  Preserve insertion order              → LinkedHashMap   O(1)");
        System.out.println("  Multi-threaded cache / counter        → ConcurrentHashMap O(1)");
        System.out.println("  LRU eviction cache                    → LinkedHashMap(accessOrder=true)");
        System.out.println("  Price range / floor-ceiling lookup    → TreeMap.floorKey/ceilingKey");
        System.out.println("\n=== END OF MAP DEEP DIVE ===");
    }
}
