import java.util.*;
import java.util.stream.*;

/**
 * ============================================================
 *  ARRAYS DEEP DIVE — Patterns, Algorithms, Real-World Use Cases
 *  Run: javac ArraysDeepDive.java && java ArraysDeepDive
 * ============================================================
 *
 *  WHAT IS AN ARRAY?
 *  ------------------
 *  An array is a fixed-size, contiguous block of memory that stores
 *  elements of the same type, each accessible in O(1) via an index.
 *
 *  MEMORY LAYOUT:
 *    Index:   0    1    2    3    4
 *    Value:  10   20   30   40   50
 *    Addr:  100  104  108  112  116  ← 4 bytes per int
 *
 *  Address formula:
 *    address = baseAddress + (index × elementSize)
 *    arr[3] → 100 + (3 × 4) = 112   ← one arithmetic op = O(1)
 *
 *  JAVA ARRAY TYPES:
 *    int[]    → primitive, values stored directly (contiguous ints)
 *    String[] → reference, array holds object references (objects on heap)
 *    int[][]  → array of arrays (each row is a separate heap object)
 *
 *  CORE COMPLEXITY:
 *    Access  O(1)  → index arithmetic
 *    Update  O(1)  → index arithmetic
 *    Search  O(n)  → linear scan (unsorted)
 *    Insert  O(n)  → must shift elements right
 *    Delete  O(n)  → must shift elements left
 *    Space   O(n)  → n elements
 */
public class ArraysDeepDive {

    public static void main(String[] args) {
        System.out.println("=== ARRAYS DEEP DIVE ===\n");

        section1_ArrayBasicsAndMemory();
        section2_CoreOperations();
        section3_TwoPointers();
        section4_SlidingWindow();
        section5_PrefixSum();
        section6_TwoDimensionalArrays();
        section7_SpiralTraversal();
        section8_RealWorldTimeSeries();
        section9_RealWorldSensorMonitoring();
        section10_RealWorldImageProcessing();
        section11_PracticeProblems();
        section12_CommonMistakes();
        section13_InterviewSummary();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 1 — Array Basics and Memory Model
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * ARRAY MEMORY MODEL
     * -------------------
     * Java arrays are objects allocated on the heap.
     * The variable (e.g. int[] arr) holds a REFERENCE to the array object.
     *
     * PRIMITIVE ARRAY:
     *   int[] nums = {10, 20, 30, 40, 50};
     *   heap: [10][20][30][40][50]   ← values stored inline
     *
     * OBJECT/REFERENCE ARRAY:
     *   String[] words = {"A", "B", "C"};
     *   heap: [ref0][ref1][ref2]     ← references stored inline
     *         ↓      ↓      ↓
     *        "A"    "B"    "C"       ← String objects elsewhere on heap
     *
     * DEFAULT VALUES (Java zero-initialises all array slots):
     *   int[]     → 0
     *   double[]  → 0.0
     *   boolean[] → false
     *   char[]    → '\u0000'
     *   Object[]  → null
     */
    static void section1_ArrayBasicsAndMemory() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 1: Array Basics and Memory Model");
        System.out.println("─────────────────────────────────────────");

        // Declaration and initialisation
        int[] zeros     = new int[5];                       // zero-initialised
        int[] withVals  = new int[]{10, 20, 30, 40, 50};   // explicit values
        int[] shorthand = {10, 20, 30, 40, 50};            // shorthand literal

        System.out.println("zero-init int[5]:  " + Arrays.toString(zeros));
        System.out.println("explicit values:   " + Arrays.toString(withVals));
        System.out.println("shorthand literal: " + Arrays.toString(shorthand));

        // Address formula demonstration
        System.out.println("\nAddress formula: addr = base + index × elementSize");
        System.out.println("Assume base=1000, elementSize=4 bytes (int):");
        for (int i = 0; i < withVals.length; i++) {
            System.out.printf("  arr[%d] = %-3d  at address %d%n",
                i, withVals[i], 1000 + i * 4);
        }

        // Primitive vs object array
        String[] refs = new String[3];
        System.out.println("\nObject array default: " + Arrays.toString(refs)); // [null, null, null]
        refs[0] = "Hello"; refs[1] = "World";
        System.out.println("After assignment:     " + Arrays.toString(refs));

        // Array is an object — has .length field (not a method)
        System.out.println("\nArray metadata:");
        System.out.println("  withVals.length = " + withVals.length);
        System.out.println("  withVals.getClass().getSimpleName() = "
            + withVals.getClass().getSimpleName());

        // Copying arrays — shallow copy (object arrays share references)
        int[] original = {1, 2, 3, 4, 5};
        int[] copy1    = original.clone();              // full copy
        int[] copy2    = Arrays.copyOf(original, 3);    // copy first 3
        int[] copy3    = Arrays.copyOfRange(original, 1, 4); // indices 1..3
        System.out.println("\nCopying:");
        System.out.println("  clone():         " + Arrays.toString(copy1));
        System.out.println("  copyOf(3):       " + Arrays.toString(copy2));
        System.out.println("  copyOfRange(1,4):" + Arrays.toString(copy3));

        // Mutation of copy does NOT affect original (for primitive arrays)
        copy1[0] = 999;
        System.out.println("  After copy1[0]=999: original=" + Arrays.toString(original)
            + "  copy1=" + Arrays.toString(copy1));
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 2 — Core Operations with Complexity
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * CORE ARRAY OPERATIONS
     * ----------------------
     * Access/Update: O(1) — index arithmetic, single memory operation
     * Linear Search: O(n) — must scan up to n elements
     * Binary Search: O(log n) — requires SORTED array; halves search space each step
     * Insert:        O(n) — must shift all elements right of insert point
     * Delete:        O(n) — must shift all elements left after deletion
     * Sort:          O(n log n) — Arrays.sort uses dual-pivot Quicksort (primitives)
     *                             or TimSort (objects)
     *
     * BINARY SEARCH CONTRACT:
     *   Array MUST be sorted before calling binarySearch.
     *   Returns: index if found, or -(insertionPoint + 1) if not found.
     *   Two equal elements: no guarantee which index is returned.
     */
    static void section2_CoreOperations() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 2: Core Operations and Complexity");
        System.out.println("─────────────────────────────────────────");

        int[] arr = {5, 3, 8, 1, 9, 2, 7, 4, 6};
        System.out.println("Array: " + Arrays.toString(arr));

        // Access O(1)
        long start = System.nanoTime();
        int val = arr[4];
        System.out.printf("\nAccess arr[4]=%d  [O(1)] %,d ns%n", val, System.nanoTime()-start);

        // Update O(1)
        start = System.nanoTime();
        arr[4] = 99;
        System.out.printf("Update arr[4]=99  [O(1)] %,d ns  → %s%n",
            System.nanoTime()-start, Arrays.toString(arr));
        arr[4] = 9; // restore

        // Linear search O(n)
        start = System.nanoTime();
        int target = 7, foundAt = -1;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == target) { foundAt = i; break; }
        }
        System.out.printf("Linear search(%d) found at index %d  [O(n)] %,d ns%n",
            target, foundAt, System.nanoTime()-start);

        // Sort O(n log n)
        int[] sorted = arr.clone();
        start = System.nanoTime();
        Arrays.sort(sorted);
        System.out.printf("Arrays.sort()     [O(n log n)] %,d ns → %s%n",
            System.nanoTime()-start, Arrays.toString(sorted));

        // Binary search O(log n) — requires sorted
        start = System.nanoTime();
        int idx = Arrays.binarySearch(sorted, 7);
        System.out.printf("binarySearch(7) found at index %d  [O(log n)] %,d ns%n",
            idx, System.nanoTime()-start);

        // Insert simulation — O(n) (shift right)
        int[] withInsert = new int[arr.length + 1];
        int insertVal = 50, insertAt = 3;
        System.arraycopy(arr, 0, withInsert, 0, insertAt);
        withInsert[insertAt] = insertVal;
        System.arraycopy(arr, insertAt, withInsert, insertAt + 1, arr.length - insertAt);
        System.out.printf("Insert %d at index %d [O(n)]: %s%n",
            insertVal, insertAt, Arrays.toString(withInsert));

        // Delete simulation — O(n) (shift left)
        int[] afterDelete = new int[arr.length - 1];
        int deleteAt = 3;
        System.arraycopy(arr, 0, afterDelete, 0, deleteAt);
        System.arraycopy(arr, deleteAt + 1, afterDelete, deleteAt, arr.length - deleteAt - 1);
        System.out.printf("Delete index %d     [O(n)]: %s%n%n", deleteAt,
            Arrays.toString(afterDelete));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 3 — Two Pointers Pattern
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * TWO POINTERS PATTERN
     * ----------------------
     * Use two indices that move toward each other (or in the same direction)
     * to reduce an O(n²) nested loop to O(n).
     *
     * VARIANTS:
     *   A) Opposite ends → converge toward centre (sorted array problems)
     *   B) Same direction → fast and slow (cycle detection, duplicates)
     *   C) Both start at 0 → fast searches without extra space
     *
     * WHEN TO USE:
     *   - Array is sorted (or can be sorted)
     *   - Looking for pairs/triplets with a constraint
     *   - Removing duplicates in-place
     *   - Merging two sorted arrays
     *
     * COMPLEXITY: O(n) time, O(1) space — each pointer moves at most n steps.
     */
    static void section3_TwoPointers() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 3: Two Pointers Pattern");
        System.out.println("─────────────────────────────────────────");

        // ── Problem 1: Find pair with target sum (sorted array) ───────────────
        int[] sortedArr = {1, 2, 3, 4, 6, 8, 9, 11};
        int   target    = 10;
        System.out.println("Problem 1 — Pair with sum " + target
            + " in " + Arrays.toString(sortedArr) + ":");
        System.out.println("  " + findPairWithSum(sortedArr, target));

        // ── Problem 2: Remove duplicates in-place from sorted array ───────────
        int[] withDups = {1, 1, 2, 3, 3, 3, 4, 5, 5};
        int unique = removeDuplicatesInPlace(withDups);
        System.out.println("\nProblem 2 — Remove duplicates from " + Arrays.toString(withDups) + ":");
        System.out.println("  Unique count=" + unique
            + "  first " + unique + " elements: "
            + Arrays.toString(Arrays.copyOf(withDups, unique)));

        // ── Problem 3: Move zeros to end ─────────────────────────────────────
        int[] withZeros = {0, 1, 0, 3, 12, 0, 5};
        System.out.println("\nProblem 3 — Move zeros in " + Arrays.toString(withZeros) + ":");
        moveZerosToEnd(withZeros);
        System.out.println("  After: " + Arrays.toString(withZeros));

        // ── Problem 4: Three-sum (all triplets that sum to 0) ─────────────────
        int[] threeArr = {-4, -1, -1, 0, 1, 2};
        System.out.println("\nProblem 4 — Three-sum (triplets = 0) in "
            + Arrays.toString(threeArr) + ":");
        threeSum(threeArr).forEach(t -> System.out.println("  " + t));

        // ── Problem 5: Reverse array in-place ────────────────────────────────
        int[] toReverse = {1, 2, 3, 4, 5};
        System.out.println("\nProblem 5 — Reverse " + Arrays.toString(toReverse) + ":");
        reverseArray(toReverse);
        System.out.println("  Reversed: " + Arrays.toString(toReverse));

        // ── Problem 6: Container with most water (LeetCode 11) ───────────────
        int[] heights = {1, 8, 6, 2, 5, 4, 8, 3, 7};
        System.out.println("\nProblem 6 — Max water container heights="
            + Arrays.toString(heights) + ":");
        System.out.println("  Max water = " + maxWaterContainer(heights));
        System.out.println();
    }

    static String findPairWithSum(int[] arr, int target) {
        int left = 0, right = arr.length - 1;
        while (left < right) {
            int sum = arr[left] + arr[right];
            if (sum == target)
                return "Found: [" + arr[left] + " + " + arr[right] + " = " + target
                    + "] at indices [" + left + ", " + right + "]";
            if (sum < target) left++;
            else              right--;
        }
        return "No pair found";
    }

    static int removeDuplicatesInPlace(int[] arr) {
        if (arr.length == 0) return 0;
        int write = 1; // slow pointer — next write position
        for (int read = 1; read < arr.length; read++) { // fast pointer
            if (arr[read] != arr[read - 1]) {
                arr[write++] = arr[read];
            }
        }
        return write;
    }

    static void moveZerosToEnd(int[] arr) {
        int write = 0;
        for (int read = 0; read < arr.length; read++) {
            if (arr[read] != 0) arr[write++] = arr[read];
        }
        while (write < arr.length) arr[write++] = 0;
    }

    static List<List<Integer>> threeSum(int[] arr) {
        Arrays.sort(arr);
        List<List<Integer>> result = new ArrayList<>();
        for (int i = 0; i < arr.length - 2; i++) {
            if (i > 0 && arr[i] == arr[i - 1]) continue; // skip duplicates
            int left = i + 1, right = arr.length - 1;
            while (left < right) {
                int sum = arr[i] + arr[left] + arr[right];
                if (sum == 0) {
                    result.add(List.of(arr[i], arr[left], arr[right]));
                    while (left < right && arr[left] == arr[left + 1]) left++;
                    while (left < right && arr[right] == arr[right - 1]) right--;
                    left++; right--;
                } else if (sum < 0) left++;
                else right--;
            }
        }
        return result;
    }

    static void reverseArray(int[] arr) {
        int left = 0, right = arr.length - 1;
        while (left < right) {
            int tmp = arr[left]; arr[left] = arr[right]; arr[right] = tmp;
            left++; right--;
        }
    }

    static int maxWaterContainer(int[] heights) {
        int left = 0, right = heights.length - 1, maxWater = 0;
        while (left < right) {
            int water = Math.min(heights[left], heights[right]) * (right - left);
            maxWater = Math.max(maxWater, water);
            if (heights[left] < heights[right]) left++;
            else right--;
        }
        return maxWater;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 4 — Sliding Window Pattern
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * SLIDING WINDOW PATTERN
     * -----------------------
     * Maintain a "window" (contiguous subarray) that slides across the array.
     * Instead of recomputing the whole window each step, add the new right element
     * and remove the old left element — O(1) update per slide.
     *
     * TWO VARIANTS:
     *
     * FIXED SIZE window (size k):
     *   - Build initial window of size k
     *   - Slide: add arr[i], remove arr[i-k]
     *   - Track max/min/sum across all windows
     *   - Time O(n), Space O(1)
     *
     * VARIABLE SIZE window:
     *   - Expand right until constraint violated
     *   - Shrink left until constraint satisfied again
     *   - Track max/min window size
     *   - Time O(n) amortized — each element enters and leaves window at most once
     *
     * WHEN TO USE:
     *   - "find the maximum/minimum/count of a contiguous subarray of size k"
     *   - "find the smallest/largest subarray with sum/condition"
     *   - "count of subarrays satisfying a constraint"
     */
    static void section4_SlidingWindow() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 4: Sliding Window Pattern");
        System.out.println("─────────────────────────────────────────");

        // ── Problem 1: Max sum subarray of size k (fixed window) ─────────────
        int[] arr1 = {2, 1, 5, 1, 3, 2};
        int k = 3;
        System.out.println("Problem 1 — Max sum of k=" + k + " in "
            + Arrays.toString(arr1) + ":");
        System.out.println("  Max sum = " + maxSumFixedWindow(arr1, k));

        // ── Problem 2: Smallest subarray with sum ≥ target (variable window) ──
        int[] arr2 = {2, 3, 1, 2, 4, 3};
        int minTarget = 7;
        System.out.println("\nProblem 2 — Min length subarray with sum≥" + minTarget
            + " in " + Arrays.toString(arr2) + ":");
        System.out.println("  Min length = " + minSubarrayLength(arr2, minTarget));

        // ── Problem 3: Longest substring without repeating characters ─────────
        String s = "abcabcbb";
        System.out.println("\nProblem 3 — Longest unique-char substring in \"" + s + "\":");
        System.out.println("  Length = " + longestUniqueSubstring(s));

        // ── Problem 4: Max distinct characters in all windows of size k ────────
        int[] arr3 = {1, 2, 1, 3, 4, 2, 3};
        int k2 = 4;
        System.out.println("\nProblem 4 — Avg of each window of size k=" + k2
            + " in " + Arrays.toString(arr3) + ":");
        System.out.println("  Averages: " + Arrays.toString(windowAverages(arr3, k2)));

        // ── Problem 5: Best time to buy and sell stock ────────────────────────
        int[] prices = {7, 1, 5, 3, 6, 4};
        System.out.println("\nProblem 5 — Best stock profit in prices="
            + Arrays.toString(prices) + ":");
        System.out.println("  Max profit = " + maxStockProfit(prices));

        // ── Problem 6: Count subarrays with sum equal to k ────────────────────
        int[] arr4 = {1, 1, 1};
        int k3 = 2;
        System.out.println("\nProblem 6 — Count subarrays with sum=" + k3
            + " in " + Arrays.toString(arr4) + ":");
        System.out.println("  Count = " + countSubarraysWithSum(arr4, k3));
        System.out.println();
    }

    static int maxSumFixedWindow(int[] arr, int k) {
        if (arr.length < k) return -1;
        int windowSum = 0;
        for (int i = 0; i < k; i++) windowSum += arr[i];
        int maxSum = windowSum;
        for (int i = k; i < arr.length; i++) {
            windowSum += arr[i] - arr[i - k]; // slide: add new, remove old
            maxSum = Math.max(maxSum, windowSum);
        }
        return maxSum;
    }

    static int minSubarrayLength(int[] arr, int target) {
        int left = 0, sum = 0, minLen = Integer.MAX_VALUE;
        for (int right = 0; right < arr.length; right++) {
            sum += arr[right]; // expand window
            while (sum >= target) {
                minLen = Math.min(minLen, right - left + 1);
                sum -= arr[left++]; // shrink window
            }
        }
        return minLen == Integer.MAX_VALUE ? 0 : minLen;
    }

    static int longestUniqueSubstring(String s) {
        Map<Character, Integer> lastSeen = new HashMap<>();
        int left = 0, maxLen = 0;
        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            if (lastSeen.containsKey(c) && lastSeen.get(c) >= left) {
                left = lastSeen.get(c) + 1; // shrink: move left past duplicate
            }
            lastSeen.put(c, right);
            maxLen = Math.max(maxLen, right - left + 1);
        }
        return maxLen;
    }

    static double[] windowAverages(int[] arr, int k) {
        double[] avgs = new double[arr.length - k + 1];
        double sum = 0;
        for (int i = 0; i < k; i++) sum += arr[i];
        avgs[0] = sum / k;
        for (int i = k; i < arr.length; i++) {
            sum += arr[i] - arr[i - k];
            avgs[i - k + 1] = sum / k;
        }
        return avgs;
    }

    static int maxStockProfit(int[] prices) {
        int minPrice = Integer.MAX_VALUE, maxProfit = 0;
        for (int price : prices) {
            minPrice  = Math.min(minPrice, price);
            maxProfit = Math.max(maxProfit, price - minPrice);
        }
        return maxProfit;
    }

    // Uses prefix-sum + HashMap for O(n) solution
    static int countSubarraysWithSum(int[] arr, int k) {
        Map<Integer, Integer> prefixCount = new HashMap<>();
        prefixCount.put(0, 1);
        int count = 0, prefixSum = 0;
        for (int val : arr) {
            prefixSum += val;
            count += prefixCount.getOrDefault(prefixSum - k, 0);
            prefixCount.merge(prefixSum, 1, Integer::sum);
        }
        return count;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 5 — Prefix Sum Pattern
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * PREFIX SUM PATTERN
     * -------------------
     * Precompute an array where prefix[i] = sum of arr[0..i-1].
     * Then any range sum query [left, right] becomes O(1):
     *   rangeSum(left, right) = prefix[right+1] - prefix[left]
     *
     * STRUCTURE (1-indexed prefix array of length n+1):
     *   arr:    [3,  1,  4,  1,  5,  9]
     *   prefix: [0,  3,  4,  8,  9, 14, 23]
     *            ↑ always 0
     *
     *   Sum of arr[2..4] = prefix[5] - prefix[2] = 14 - 4 = 10 ✓
     *
     * BUILD:   O(n) — one pass
     * QUERY:   O(1) — two array lookups + subtraction
     * SPACE:   O(n) — the prefix array
     *
     * EXTENSIONS:
     *   2D prefix sum → O(1) rectangle sum queries
     *   Prefix XOR    → O(1) range XOR queries
     *   Prefix product → O(1) range product (careful with zeros)
     */
    static void section5_PrefixSum() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 5: Prefix Sum Pattern");
        System.out.println("─────────────────────────────────────────");

        int[] arr = {3, 1, 4, 1, 5, 9, 2, 6};
        int[] prefix = buildPrefix(arr);
        System.out.println("Array:  " + Arrays.toString(arr));
        System.out.println("Prefix: " + Arrays.toString(prefix));

        // Range sum queries O(1)
        System.out.println("\nRange sum queries O(1):");
        System.out.printf("  sum[0..3] = %d%n", rangeSum(prefix, 0, 3));
        System.out.printf("  sum[2..5] = %d%n", rangeSum(prefix, 2, 5));
        System.out.printf("  sum[0..7] = %d (total)%n", rangeSum(prefix, 0, 7));

        // ── Problem: Subarray sum equals k ────────────────────────────────────
        int[] arr2 = {1, -1, 5, -2, 3};
        int k = 3;
        System.out.println("\nSubarray sum = " + k + " in " + Arrays.toString(arr2) + ":");
        System.out.println("  Count = " + countSubarraysWithSumK(arr2, k));

        // ── Problem: Equilibrium index (left sum = right sum) ─────────────────
        int[] arr3 = {1, 7, 3, 6, 5, 6};
        System.out.println("\nEquilibrium index in " + Arrays.toString(arr3) + ":");
        System.out.println("  Index = " + equilibriumIndex(arr3));

        // ── Problem: Product of array except self (no division) ───────────────
        int[] arr4 = {1, 2, 3, 4};
        System.out.println("\nProduct except self for " + Arrays.toString(arr4) + ":");
        System.out.println("  Result = " + Arrays.toString(productExceptSelf(arr4)));

        // ── Problem: Maximum subarray sum (Kadane's algorithm) ─────────────────
        int[] arr5 = {-2, 1, -3, 4, -1, 2, 1, -5, 4};
        System.out.println("\nMax subarray sum (Kadane's) in " + Arrays.toString(arr5) + ":");
        System.out.println("  Max sum = " + maxSubarraySum(arr5));

        // ── 2D Prefix Sum ─────────────────────────────────────────────────────
        int[][] matrix = {
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}
        };
        int[][] prefix2d = build2DPrefix(matrix);
        System.out.println("\n2D Prefix Sum — rectangle query O(1):");
        System.out.println("  Matrix:");
        for (int[] row : matrix) System.out.println("    " + Arrays.toString(row));
        // Sum of rectangle (0,0) to (1,1) = 1+2+4+5 = 12
        System.out.println("  Sum of rectangle (0,0)→(1,1) = "
            + rangeSum2D(prefix2d, 0, 0, 1, 1));
        System.out.println("  Sum of rectangle (1,1)→(2,2) = "
            + rangeSum2D(prefix2d, 1, 1, 2, 2));
        System.out.println();
    }

    static int[] buildPrefix(int[] arr) {
        int[] prefix = new int[arr.length + 1];
        for (int i = 0; i < arr.length; i++)
            prefix[i + 1] = prefix[i] + arr[i];
        return prefix;
    }

    static int rangeSum(int[] prefix, int left, int right) {
        return prefix[right + 1] - prefix[left];
    }

    static int countSubarraysWithSumK(int[] arr, int k) {
        Map<Integer, Integer> freq = new HashMap<>();
        freq.put(0, 1);
        int count = 0, sum = 0;
        for (int val : arr) {
            sum += val;
            count += freq.getOrDefault(sum - k, 0);
            freq.merge(sum, 1, Integer::sum);
        }
        return count;
    }

    static int equilibriumIndex(int[] arr) {
        int total = 0;
        for (int v : arr) total += v;
        int leftSum = 0;
        for (int i = 0; i < arr.length; i++) {
            int rightSum = total - leftSum - arr[i];
            if (leftSum == rightSum) return i;
            leftSum += arr[i];
        }
        return -1;
    }

    static int[] productExceptSelf(int[] arr) {
        int n = arr.length;
        int[] result = new int[n];
        result[0] = 1;
        for (int i = 1; i < n; i++)           // left products
            result[i] = result[i - 1] * arr[i - 1];
        int right = 1;
        for (int i = n - 1; i >= 0; i--) {    // multiply right products
            result[i] *= right;
            right *= arr[i];
        }
        return result;
    }

    static int maxSubarraySum(int[] arr) {
        int maxSoFar = arr[0], maxEndingHere = arr[0];
        for (int i = 1; i < arr.length; i++) {
            maxEndingHere = Math.max(arr[i], maxEndingHere + arr[i]);
            maxSoFar      = Math.max(maxSoFar, maxEndingHere);
        }
        return maxSoFar;
    }

    static int[][] build2DPrefix(int[][] mat) {
        int rows = mat.length, cols = mat[0].length;
        int[][] p = new int[rows + 1][cols + 1];
        for (int i = 1; i <= rows; i++)
            for (int j = 1; j <= cols; j++)
                p[i][j] = mat[i-1][j-1] + p[i-1][j] + p[i][j-1] - p[i-1][j-1];
        return p;
    }

    static int rangeSum2D(int[][] p, int r1, int c1, int r2, int c2) {
        return p[r2+1][c2+1] - p[r1][c2+1] - p[r2+1][c1] + p[r1][c1];
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 6 — 2D Arrays
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * 2D ARRAYS IN JAVA
     * ------------------
     * Java 2D arrays are ARRAYS OF ARRAYS (jagged arrays are allowed):
     *
     *   int[][] matrix = new int[3][4];
     *
     * Memory layout:
     *   matrix → [ref_row0 | ref_row1 | ref_row2]   (outer array on heap)
     *                ↓           ↓           ↓
     *            [int,int,int,int]  ...          (each row is a separate heap object)
     *
     * This means:
     *   - matrix.length      → number of rows
     *   - matrix[i].length   → number of columns in row i (can differ!)
     *   - matrix[i][j]       → element at row i, column j
     *   - Access is O(1) — two pointer dereferences
     *
     * COMMON TRAVERSALS:
     *   Row-by-row        → for (i) for (j) — cache friendly
     *   Column-by-column  → for (j) for (i) — cache unfriendly (row-major layout)
     *   Diagonal          → i == j (main), i+j == n-1 (anti)
     *   Transpose         → swap [i][j] with [j][i]
     */
    static void section6_TwoDimensionalArrays() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 6: 2D Arrays");
        System.out.println("─────────────────────────────────────────");

        int[][] matrix = {
            { 1,  2,  3,  4},
            { 5,  6,  7,  8},
            { 9, 10, 11, 12},
            {13, 14, 15, 16}
        };

        System.out.println("Matrix (" + matrix.length + "×" + matrix[0].length + "):");
        printMatrix(matrix);

        // Row-by-row sum
        System.out.println("\nRow sums:");
        for (int i = 0; i < matrix.length; i++) {
            int rowSum = 0;
            for (int j = 0; j < matrix[i].length; j++) rowSum += matrix[i][j];
            System.out.println("  Row " + i + ": " + rowSum);
        }

        // Main diagonal
        System.out.print("\nMain diagonal (i==j):     ");
        for (int i = 0; i < matrix.length; i++) System.out.print(matrix[i][i] + " ");

        // Anti-diagonal
        System.out.print("\nAnti-diagonal (i+j==n-1): ");
        for (int i = 0; i < matrix.length; i++)
            System.out.print(matrix[i][matrix.length - 1 - i] + " ");
        System.out.println();

        // Transpose
        int[][] transposed = transpose(matrix);
        System.out.println("\nTransposed:");
        printMatrix(transposed);

        // Rotate 90° clockwise
        int[][] rotated = rotate90CW(matrix);
        System.out.println("\nRotated 90° clockwise:");
        printMatrix(rotated);

        // Set matrix zeroes — if element is 0, set entire row and column to 0
        int[][] withZero = {
            {1, 1, 1},
            {1, 0, 1},
            {1, 1, 1}
        };
        System.out.println("\nSet zeroes — before:");
        printMatrix(withZero);
        setZeroes(withZero);
        System.out.println("Set zeroes — after:");
        printMatrix(withZero);
        System.out.println();
    }

    static void printMatrix(int[][] m) {
        for (int[] row : m) {
            System.out.print("  [");
            for (int j = 0; j < row.length; j++)
                System.out.printf("%3d%s", row[j], j < row.length-1 ? "," : "");
            System.out.println("]");
        }
    }

    static int[][] transpose(int[][] m) {
        int rows = m.length, cols = m[0].length;
        int[][] t = new int[cols][rows];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                t[j][i] = m[i][j];
        return t;
    }

    static int[][] rotate90CW(int[][] m) {
        int n = m.length;
        int[][] r = new int[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                r[j][n - 1 - i] = m[i][j];
        return r;
    }

    static void setZeroes(int[][] m) {
        boolean firstRowZero = false, firstColZero = false;
        for (int j = 0; j < m[0].length; j++) if (m[0][j] == 0) firstRowZero = true;
        for (int i = 0; i < m.length;    i++) if (m[i][0] == 0) firstColZero = true;
        for (int i = 1; i < m.length;    i++)
            for (int j = 1; j < m[0].length; j++)
                if (m[i][j] == 0) { m[i][0] = 0; m[0][j] = 0; }
        for (int i = 1; i < m.length;    i++)
            for (int j = 1; j < m[0].length; j++)
                if (m[i][0] == 0 || m[0][j] == 0) m[i][j] = 0;
        if (firstRowZero) Arrays.fill(m[0], 0);
        if (firstColZero) for (int i = 0; i < m.length; i++) m[i][0] = 0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 7 — Spiral Traversal
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * SPIRAL TRAVERSAL
     * -----------------
     * Traverse a 2D matrix in clockwise spiral order: right → down → left → up.
     * Maintain four boundaries: top, bottom, left, right.
     * After each direction traversal, shrink the corresponding boundary.
     *
     * Visual for 4×4:
     *   →  →  →  →
     *             ↓
     *   ↑  →  →  ↓
     *   ↑  ↑     ↓
     *   ↑  ←  ←  ←
     *
     * Time:  O(n × m) — every element visited exactly once
     * Space: O(1) extra — result list excluded from space count
     */
    static void section7_SpiralTraversal() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 7: Spiral Traversal");
        System.out.println("─────────────────────────────────────────");

        int[][] matrix4x4 = {
            { 1,  2,  3,  4},
            { 5,  6,  7,  8},
            { 9, 10, 11, 12},
            {13, 14, 15, 16}
        };
        System.out.println("4×4 matrix:");
        printMatrix(matrix4x4);
        System.out.println("Spiral order: " + spiralOrder(matrix4x4));

        int[][] matrix3x5 = {
            { 1,  2,  3,  4,  5},
            { 6,  7,  8,  9, 10},
            {11, 12, 13, 14, 15}
        };
        System.out.println("\n3×5 matrix:");
        printMatrix(matrix3x5);
        System.out.println("Spiral order: " + spiralOrder(matrix3x5));

        // Spiral fill — fill a matrix in spiral order with 1..n*m
        int n = 4;
        int[][] filled = spiralFill(n);
        System.out.println("\nSpiral-filled " + n + "×" + n + " matrix:");
        printMatrix(filled);
        System.out.println();
    }

    static List<Integer> spiralOrder(int[][] matrix) {
        List<Integer> result = new ArrayList<>();
        int top = 0, bottom = matrix.length - 1;
        int left = 0, right = matrix[0].length - 1;
        while (top <= bottom && left <= right) {
            for (int j = left;  j <= right;  j++) result.add(matrix[top][j]);  top++;
            for (int i = top;   i <= bottom; i++) result.add(matrix[i][right]); right--;
            if (top <= bottom)
                for (int j = right; j >= left;  j--) result.add(matrix[bottom][j]); bottom--;
            if (left <= right)
                for (int i = bottom; i >= top;  i--) result.add(matrix[i][left]);   left++;
        }
        return result;
    }

    static int[][] spiralFill(int n) {
        int[][] m = new int[n][n];
        int top = 0, bottom = n-1, left = 0, right = n-1, num = 1;
        while (top <= bottom && left <= right) {
            for (int j = left;  j <= right;  j++) m[top][j]    = num++;  top++;
            for (int i = top;   i <= bottom; i++) m[i][right]  = num++;  right--;
            if (top <= bottom)
                for (int j = right; j >= left; j--) m[bottom][j] = num++;  bottom--;
            if (left <= right)
                for (int i = bottom; i >= top; i--) m[i][left]   = num++;  left++;
        }
        return m;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 8 — Real World: Time Series (Stock Prices)
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * REAL WORLD — TIME-SERIES ANALYTICS
     * ------------------------------------
     * Stock prices, sensor readings, log metrics — all are time-series arrays.
     * Common operations:
     *   - Moving average        → sliding window sum / k
     *   - Running max/min       → running extremes
     *   - Percentage change     → (curr - prev) / prev * 100
     *   - Bollinger bands       → mean ± 2 * stddev over window
     *   - Range queries         → prefix sum for sum over any [l, r] interval
     */
    static void section8_RealWorldTimeSeries() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 8: Real World — Time Series (Stock Prices)");
        System.out.println("─────────────────────────────────────────");

        double[] prices = {100.0, 102.5, 101.0, 105.0, 108.5, 107.0, 110.0, 112.5, 111.0, 115.0};
        System.out.println("Daily prices: " + Arrays.toString(prices));

        // Moving average (window=3) — sliding window
        int w = 3;
        System.out.print("Moving average (k=" + w + "): ");
        double[] ma = movingAverage(prices, w);
        System.out.printf("[");
        for (int i = 0; i < ma.length; i++)
            System.out.printf("%.1f%s", ma[i], i < ma.length-1 ? ", " : "");
        System.out.println("]");

        // Daily returns (percentage change)
        System.out.print("Daily returns (%): [");
        for (int i = 1; i < prices.length; i++) {
            double ret = (prices[i] - prices[i-1]) / prices[i-1] * 100;
            System.out.printf("%+.1f%s", ret, i < prices.length-1 ? ", " : "");
        }
        System.out.println("]");

        // Running max — O(n) sliding window max
        System.out.print("Running max:      ");
        double runMax = prices[0];
        for (double p : prices) { runMax = Math.max(runMax, p); System.out.printf("%.0f ", p > runMax-1 ? runMax : runMax); }
        System.out.println();

        // Max profit (best buy-sell)
        System.out.println("Max profit: $"
            + String.format("%.1f", maxDoubleProfit(prices)));

        // Prefix sum for range sum queries
        double[] prefD = new double[prices.length + 1];
        for (int i = 0; i < prices.length; i++) prefD[i+1] = prefD[i] + prices[i];
        System.out.printf("Sum of prices[3..6]: $%.1f%n", prefD[7] - prefD[3]);
        System.out.println();
    }

    static double[] movingAverage(double[] arr, int k) {
        double[] ma = new double[arr.length - k + 1];
        double sum = 0;
        for (int i = 0; i < k; i++) sum += arr[i];
        ma[0] = sum / k;
        for (int i = k; i < arr.length; i++) {
            sum += arr[i] - arr[i - k];
            ma[i - k + 1] = sum / k;
        }
        return ma;
    }

    static double maxDoubleProfit(double[] prices) {
        double minP = prices[0], maxProfit = 0;
        for (double p : prices) {
            minP = Math.min(minP, p);
            maxProfit = Math.max(maxProfit, p - minP);
        }
        return maxProfit;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 9 — Real World: Sensor Monitoring
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * REAL WORLD — SENSOR / LOG AGGREGATION
     * ---------------------------------------
     * IoT sensors emit readings at fixed intervals.
     * Operations required by monitoring systems:
     *   - Detect anomalies (reading deviates > threshold from window average)
     *   - Find the peak reading within a time window
     *   - Count alerts in the last N minutes (sliding window count)
     *   - Compute hourly averages (fixed-window reduction)
     */
    static void section9_RealWorldSensorMonitoring() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 9: Real World — Sensor Monitoring");
        System.out.println("─────────────────────────────────────────");

        double[] temps = {20.1, 20.5, 21.0, 20.8, 35.2, 21.1, 20.9, 22.0, 21.5, 21.8};
        System.out.println("Temperature readings: " + Arrays.toString(temps));

        // Anomaly detection: reading > windowAvg + threshold triggers alert
        int windowSize = 3;
        double threshold = 5.0;
        System.out.println("\nAnomalies (deviation > " + threshold + " from window avg):");
        for (int i = windowSize; i < temps.length; i++) {
            double windowAvg = 0;
            for (int j = i - windowSize; j < i; j++) windowAvg += temps[j];
            windowAvg /= windowSize;
            if (Math.abs(temps[i] - windowAvg) > threshold) {
                System.out.printf("  ⚠ Index %d: reading=%.1f  windowAvg=%.1f  delta=%.1f%n",
                    i, temps[i], windowAvg, temps[i] - windowAvg);
            }
        }

        // Hourly reduction: batch into windows of size 5
        System.out.println("\nHourly average (batch size=5):");
        int batch = 5;
        for (int start = 0; start < temps.length; start += batch) {
            int end = Math.min(start + batch, temps.length);
            double avg = 0;
            for (int i = start; i < end; i++) avg += temps[i];
            avg /= (end - start);
            System.out.printf("  Batch [%d..%d]: avg=%.2f°C%n", start, end-1, avg);
        }

        // Peak detection: local maximum in 1D
        System.out.println("\nPeak readings (local maxima):");
        for (int i = 1; i < temps.length - 1; i++) {
            if (temps[i] > temps[i-1] && temps[i] > temps[i+1])
                System.out.printf("  Peak at index %d: %.1f°C%n", i, temps[i]);
        }
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 10 — Real World: Image Processing (2D Matrix)
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * REAL WORLD — IMAGE PROCESSING
     * --------------------------------
     * Grayscale images are 2D arrays where each cell is a pixel intensity 0–255.
     * Common operations:
     *   - Flip horizontally/vertically → array reversal
     *   - Rotate 90°                   → transpose + reverse
     *   - Box blur                     → 2D sliding window average
     *   - Threshold/binarise           → compare each pixel to cutoff
     *   - Brighten/darken              → add/subtract constant, clamp to [0,255]
     */
    static void section10_RealWorldImageProcessing() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 10: Real World — Image Processing");
        System.out.println("─────────────────────────────────────────");

        int[][] image = {
            { 80, 120, 200,  50},
            {100, 150,  30, 220},
            { 60,  90, 170, 140},
            {210,  40, 110, 180}
        };
        System.out.println("Original image (pixel intensities):");
        printMatrix(image);

        // Flip horizontally (mirror left-right)
        int[][] flippedH = flipHorizontal(image);
        System.out.println("\nFlip horizontal:");
        printMatrix(flippedH);

        // Brighten: add 30 to each pixel, clamp at 255
        int[][] brightened = adjustBrightness(image, 30);
        System.out.println("\nBrightened (+30, clamped to 255):");
        printMatrix(brightened);

        // Threshold: pixels >= 128 → 255 (white), < 128 → 0 (black)
        int[][] binary = threshold(image, 128);
        System.out.println("\nBinary threshold (≥128→255):");
        printMatrix(binary);

        // Box blur (3×3 average)
        int[][] blurred = boxBlur(image, 1); // radius 1 = 3×3 kernel
        System.out.println("\nBox blur (3×3 kernel):");
        printMatrix(blurred);
        System.out.println();
    }

    static int[][] flipHorizontal(int[][] m) {
        int rows = m.length, cols = m[0].length;
        int[][] out = new int[rows][cols];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                out[i][j] = m[i][cols - 1 - j];
        return out;
    }

    static int[][] adjustBrightness(int[][] m, int delta) {
        int rows = m.length, cols = m[0].length;
        int[][] out = new int[rows][cols];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                out[i][j] = Math.min(255, Math.max(0, m[i][j] + delta));
        return out;
    }

    static int[][] threshold(int[][] m, int cut) {
        int rows = m.length, cols = m[0].length;
        int[][] out = new int[rows][cols];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                out[i][j] = m[i][j] >= cut ? 255 : 0;
        return out;
    }

    static int[][] boxBlur(int[][] m, int r) {
        int rows = m.length, cols = m[0].length;
        int[][] out = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int sum = 0, count = 0;
                for (int di = -r; di <= r; di++)
                    for (int dj = -r; dj <= r; dj++) {
                        int ni = i + di, nj = j + dj;
                        if (ni >= 0 && ni < rows && nj >= 0 && nj < cols) {
                            sum += m[ni][nj]; count++;
                        }
                    }
                out[i][j] = sum / count;
            }
        }
        return out;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 11 — Practice Problems
    // ─────────────────────────────────────────────────────────────────────────
    static void section11_PracticeProblems() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 11: Practice Problems");
        System.out.println("─────────────────────────────────────────");

        // Easy: Find second largest
        int[] arr1 = {12, 35, 1, 10, 34, 1};
        System.out.println("Second largest in " + Arrays.toString(arr1) + ": "
            + secondLargest(arr1));

        // Easy: Find missing number (0..n) → XOR trick
        int[] arr2 = {3, 0, 1};
        System.out.println("Missing number in [0.." + arr2.length + "] "
            + Arrays.toString(arr2) + ": " + missingNumber(arr2));

        // Medium: Rotate array right by k
        int[] arr3 = {1, 2, 3, 4, 5, 6, 7};
        rotateRight(arr3, 3);
        System.out.println("Rotate [1..7] right by 3: " + Arrays.toString(arr3));

        // Medium: Merge sorted arrays
        int[] a = {1, 3, 5, 7}, b = {2, 4, 6, 8};
        System.out.println("Merge sorted " + Arrays.toString(a)
            + " + " + Arrays.toString(b) + " = "
            + Arrays.toString(mergeSorted(a, b)));

        // Medium: Find all duplicates
        int[] arr4 = {4, 3, 2, 7, 8, 2, 3, 1};
        System.out.println("Duplicates in " + Arrays.toString(arr4) + ": "
            + findDuplicates(arr4));

        // Advanced: Trapping rain water
        int[] bars = {0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1};
        System.out.println("Trapped rainwater in " + Arrays.toString(bars)
            + ": " + trapRainWater(bars));

        // Advanced: Jump game (can you reach the end?)
        int[] jumps1 = {2, 3, 1, 1, 4};
        int[] jumps2 = {3, 2, 1, 0, 4};
        System.out.println("Jump game " + Arrays.toString(jumps1) + ": " + canJump(jumps1));
        System.out.println("Jump game " + Arrays.toString(jumps2) + ": " + canJump(jumps2));
        System.out.println();
    }

    static int secondLargest(int[] arr) {
        int first = Integer.MIN_VALUE, second = Integer.MIN_VALUE;
        for (int v : arr) {
            if (v > first)  { second = first; first = v; }
            else if (v > second && v != first) second = v;
        }
        return second;
    }

    static int missingNumber(int[] arr) {
        int xor = 0;
        for (int i = 0; i <= arr.length; i++) xor ^= i;
        for (int v : arr) xor ^= v;
        return xor;
    }

    static void rotateRight(int[] arr, int k) {
        k %= arr.length;
        reverseRange(arr, 0, arr.length - 1);
        reverseRange(arr, 0, k - 1);
        reverseRange(arr, k, arr.length - 1);
    }

    static void reverseRange(int[] arr, int l, int r) {
        while (l < r) { int t = arr[l]; arr[l++] = arr[r]; arr[r--] = t; }
    }

    static int[] mergeSorted(int[] a, int[] b) {
        int[] out = new int[a.length + b.length];
        int i = 0, j = 0, k = 0;
        while (i < a.length && j < b.length)
            out[k++] = (a[i] <= b[j]) ? a[i++] : b[j++];
        while (i < a.length) out[k++] = a[i++];
        while (j < b.length) out[k++] = b[j++];
        return out;
    }

    static List<Integer> findDuplicates(int[] arr) {
        List<Integer> dups = new ArrayList<>();
        for (int v : arr) {
            int idx = Math.abs(v) - 1;
            if (arr[idx] < 0) dups.add(Math.abs(v));
            else arr[idx] = -arr[idx];
        }
        for (int i = 0; i < arr.length; i++) arr[i] = Math.abs(arr[i]); // restore
        return dups;
    }

    static int trapRainWater(int[] height) {
        int n = height.length;
        int[] leftMax = new int[n], rightMax = new int[n];
        leftMax[0] = height[0];
        for (int i = 1; i < n; i++) leftMax[i] = Math.max(leftMax[i-1], height[i]);
        rightMax[n-1] = height[n-1];
        for (int i = n-2; i >= 0; i--) rightMax[i] = Math.max(rightMax[i+1], height[i]);
        int water = 0;
        for (int i = 0; i < n; i++)
            water += Math.min(leftMax[i], rightMax[i]) - height[i];
        return water;
    }

    static boolean canJump(int[] nums) {
        int maxReach = 0;
        for (int i = 0; i < nums.length; i++) {
            if (i > maxReach) return false;
            maxReach = Math.max(maxReach, i + nums[i]);
        }
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 12 — Common Mistakes
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * COMMON ARRAY MISTAKES
     * ----------------------
     * 1. Off-by-one errors (< vs <=, length vs length-1)
     * 2. ArrayIndexOutOfBoundsException — accessing arr[-1] or arr[n]
     * 3. Integer overflow in sum/product (int max = 2^31 - 1 = ~2.1 billion)
     * 4. Empty array not handled before accessing arr[0]
     * 5. Modifying array during iteration causing logic bugs
     * 6. Confusing .length (arrays) with .size() (collections)
     * 7. Shallow copy of object array — elements are shared references
     */
    static void section12_CommonMistakes() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 12: Common Mistakes");
        System.out.println("─────────────────────────────────────────");

        // Mistake 1 — ArrayIndexOutOfBoundsException
        System.out.println("Mistake 1: Off-by-one / AIOOBE");
        int[] arr = {1, 2, 3, 4, 5};
        try { int v = arr[arr.length]; }
        catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("  ❌ arr[arr.length] → AIOOBE (valid indices: 0 to " + (arr.length-1) + ")");
        }
        System.out.println("  ✅ arr[arr.length - 1] = " + arr[arr.length - 1]);

        // Mistake 2 — integer overflow
        System.out.println("\nMistake 2: Integer overflow");
        int a = Integer.MAX_VALUE, b = 1;
        System.out.println("  ❌ MAX_VALUE + 1 = " + (a + b) + " (overflow wraps negative!)");
        System.out.println("  ✅ Use long:  (long) MAX_VALUE + 1 = " + ((long) a + b));
        System.out.println("  ✅ Midpoint:  (left + right) might overflow → use left + (right-left)/2");

        // Mistake 3 — empty array
        System.out.println("\nMistake 3: Empty array guard");
        int[] empty = {};
        try { int v = empty[0]; }
        catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("  ❌ empty[0] → AIOOBE — always check arr.length == 0 first");
        }
        System.out.println("  ✅ Guard: if (arr == null || arr.length == 0) return;");

        // Mistake 4 — shallow copy of object array
        System.out.println("\nMistake 4: Shallow copy of object array");
        int[][] original = {{1, 2}, {3, 4}};
        int[][] shallowCopy = original.clone(); // copies row references, not row contents
        shallowCopy[0][0] = 999;
        System.out.println("  After shallowCopy[0][0]=999, original[0][0]="
            + original[0][0] + " ← modified! (shared row reference)");
        System.out.println("  ✅ Deep copy: Arrays.stream(original).map(int[]::clone).toArray(int[][]::new)");

        // Restore
        original[0][0] = 1;

        // Mistake 5 — .length vs .size() confusion
        System.out.println("\nMistake 5: .length vs .size()");
        int[] primitiveArr = {1, 2, 3};
        List<Integer> list   = List.of(1, 2, 3);
        System.out.println("  int[].length  = " + primitiveArr.length + " ← field, no ()");
        System.out.println("  List.size()   = " + list.size() + " ← method, requires ()");
        System.out.println("  String.length() = " + "hello".length() + " ← method");

        // Mistake 6 — midpoint overflow in binary search
        System.out.println("\nMistake 6: Midpoint calculation overflow");
        int left = 0, right = Integer.MAX_VALUE - 1;
        int badMid  = (left + right) / 2; // might overflow if both large
        int goodMid = left + (right - left) / 2;
        System.out.println("  ❌ (left+right)/2 when both large → potential overflow");
        System.out.println("  ✅ left + (right-left)/2 = " + goodMid + " (safe)");
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 13 — Interview Summary
    // ─────────────────────────────────────────────────────────────────────────
    static void section13_InterviewSummary() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("SECTION 13: Interview Cheat Sheet");
        System.out.println("─────────────────────────────────────────");

        System.out.println("""
                ARRAY FUNDAMENTALS
                  Fixed-size, contiguous memory, O(1) access by index
                  address = base + index × elementSize
                  Java: primitive array stores values; object array stores references
                  Default values: int→0, boolean→false, Object→null
                  Array is an object → has .length field (not .size())

                COMPLEXITY QUICK REFERENCE
                  Access/Update    O(1)       index arithmetic
                  Linear search    O(n)       unsorted scan
                  Binary search    O(log n)   REQUIRES sorted array
                  Insert (shift)   O(n)       elements shift right
                  Delete (shift)   O(n)       elements shift left
                  Sort (dual-QS)   O(n log n) Arrays.sort primitives
                  Sort (TimSort)   O(n log n) Arrays.sort objects
                  Space            O(n)       n elements

                TWO POINTERS
                  Opposite ends:  sorted pair sum, reverse, container water
                  Same direction: remove duplicates, move zeros (fast/slow)
                  Three-sum:      sort + fix one + two-pointer on rest
                  Template:       left=0, right=n-1; while(left<right) { ... }
                  Complexity:     O(n) time, O(1) space

                SLIDING WINDOW
                  Fixed:    build first window, slide by +arr[i] -arr[i-k]
                  Variable: expand right, shrink left when constraint violated
                  Key:      each element enters and leaves window at most once
                  Complexity: O(n) time, O(1) space (or O(k) for freq map)

                PREFIX SUM
                  prefix[0] = 0;  prefix[i+1] = prefix[i] + arr[i]
                  rangeSum(l,r) = prefix[r+1] - prefix[l]
                  Build O(n), Query O(1)
                  2D:  prefix[i][j] = cell + row above + col left - diagonal
                  Variants: prefix XOR, prefix product, prefix count

                2D ARRAYS
                  int[][] m = new int[rows][cols]
                  m.length = rows;  m[i].length = cols in row i
                  Row-major traversal (cache-friendly): for(i) for(j)
                  Transpose: out[j][i] = m[i][j]
                  Rotate CW 90°: transpose then reverse each row
                  Spiral:   four boundaries top/bottom/left/right, shrink each pass

                GOLDEN RULES
                  1.  Check for null and empty array at the start of every method
                  2.  Use left + (right - left) / 2 for midpoint (prevents overflow)
                  3.  Use long when intermediate products/sums may exceed Integer.MAX_VALUE
                  4.  Prefix sum for any range-query problem
                  5.  Sliding window for any contiguous-subarray optimisation
                  6.  Two pointers to reduce O(n²) nested loops to O(n)
                  7.  Sort first if two-pointer requires sorted input
                  8.  In-place algorithms: use the array itself as a hash (negate trick)
                  9.  Spiral: maintain four boundaries, shrink after each direction
                  10. Deep copy 2D arrays row by row — clone() on outer is shallow
                """);

        System.out.println("Q&A:");
        System.out.println("  Q: Why is array access O(1)?");
        System.out.println("     Because the address is computed by a formula — no search needed.");
        System.out.println("  Q: Why is insert O(n)?");
        System.out.println("     All elements after the insert position must shift right.");
        System.out.println("  Q: When does two-pointer reduce O(n²) to O(n)?");
        System.out.println("     When the array is sorted and each pointer moves at most n steps.");
        System.out.println("  Q: What is the key insight of prefix sum?");
        System.out.println("     Precompute once O(n), answer unlimited range queries in O(1).");
        System.out.println("  Q: Why is sliding window O(n) not O(nk)?");
        System.out.println("     Each element enters and leaves the window at most once.");
        System.out.println("\n=== END OF ARRAYS DEEP DIVE ===");
    }
}
