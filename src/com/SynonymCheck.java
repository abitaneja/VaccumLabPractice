package com;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SynonymCheck {
    private static final String NUMBER_MATCHER = "\\d+";
    private static final String DELIMITER = " ";
    private static final String SYNONYM_KEY = "synonym";
    private static final String TEST_CASE_KEY = "testCase";
    private static final String SYNONYMS = "synonyms";
    private static final String DIFFERENT = "different";


    public static void main(String[] args) {
        List<String> fileContent = readFile(args[0]);
        int totalPhases = extractTotalNumberOfTestCases(fileContent.get(0));
        AtomicInteger index = new AtomicInteger(1);
        IntStream.range(0, totalPhases).forEach(i -> {
            process(fileContent, index);
        });

    }

    private static List<String> readFile(String fileName) {
        List<String> list = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(fileName))) {
            list = br.lines().collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    private static void process(List<String> fileContent, AtomicInteger index) {
        Map<String, List<String>> rawPartitionMap = segregateSynonymsVsTestCases(fileContent, index);
        Map<String, Set<String>> synonymStore = constructSynonymStore(rawPartitionMap.get(SYNONYM_KEY));
        List<String> testCaseResults = runTestCases(rawPartitionMap.get(TEST_CASE_KEY), synonymStore);
        testCaseResults.stream().forEachOrdered(System.out::println);
    }

    private static List<String> runTestCases(List<String> testCaseList, Map<String, Set<String>> synonymStore) {
        List<String> result = new ArrayList<>();
        for (String line : testCaseList) {
            String[] testCase = line.split(DELIMITER);
            if (testCase.length < 2) {
                throw new IllegalArgumentException("Must be a Pair");
            } else {
                result.add(verifyConstraintsAndGet(testCase, synonymStore));
            }
        }
        return result;
    }

    private static String verifyConstraintsAndGet(String[] testCase, Map<String, Set<String>> synonymStore) {
        String val1 = testCase[0].toLowerCase();
        String val2 = testCase[1].toLowerCase();
        if (val1.equalsIgnoreCase(val2)) {
            return SYNONYMS;
        } else if (synonymStore.containsKey(val1) && synonymStore.containsKey(val2)) {
            boolean isRelation = checkIfDirectRelation(val1, val2, synonymStore) || checkIfInDirectRelation(val1, val2, synonymStore);
            return isRelation ? SYNONYMS : DIFFERENT;
        } else {
            return DIFFERENT;
        }
    }

    private static boolean checkIfDirectRelation(String val1, String val2, Map<String, Set<String>> synonymStore) {
        Set<String> set1 = synonymStore.get(val1);
        Set<String> set2 = synonymStore.get(val2);
        return checkIfRelation(set1, set2);
    }

    private static boolean checkIfInDirectRelation(String val1, String val2, Map<String, Set<String>> synonymStore) {
        Set<String> set1_level = synonymStore.get(val1).stream().flatMap(key -> synonymStore.get(key).stream()).collect(Collectors.toSet());
        Set<String> set2_level = synonymStore.get(val2).stream().flatMap(key -> synonymStore.get(key).stream()).collect(Collectors.toSet());
        return checkIfRelation(set1_level, set2_level);
    }

    private static boolean checkIfRelation(Set<String> s1, Set<String> s2) {
        Set<String> s1_new = new HashSet<>(s1);
        Set<String> s2_new = new HashSet<>(s2);
        s1_new.retainAll(s2_new);
        return s1_new.size() > 1 ? true : false;
    }

    private static Map<String, Set<String>> constructSynonymStore(List<String> synonymList) {
        ConcurrentHashMap<String, Set<String>> synonymStore = new ConcurrentHashMap<>();

        for (String line : synonymList) {
            String[] str = line.split(DELIMITER);
            if (str.length < 2) {
                throw new IllegalArgumentException("Must be a Pair");
            } else {
                String val1 = str[0].toLowerCase();
                String val2 = str[1].toLowerCase();
                if (synonymStore.containsKey(val1) && synonymStore.containsKey(val2)) {

                    Set<String> merge = new HashSet<>();
                    merge.addAll(synonymStore.get(val1));
                    merge.addAll(synonymStore.get(val2));
                    synonymStore.put(val1, mergeAndReturn(val2, merge));
                    synonymStore.put(val2, mergeAndReturn(val1, merge));
                } else if (synonymStore.containsKey(val1) && !synonymStore.containsKey(val2)) {
                    synonymStore.put(val1, mergeAndReturn(val2, synonymStore.get(val1)));
                    synonymStore.put(val2, mergeAndReturn(val1, synonymStore.get(val1)));

                } else if (synonymStore.containsKey(val2) && !synonymStore.containsKey(val1)) {
                    synonymStore.put(val2, mergeAndReturn(val1, synonymStore.get(val2)));
                    synonymStore.put(val1, mergeAndReturn(val2, synonymStore.get(val2)));
                } else {
                    Set<String> temp = new HashSet<>();
                    temp.add(val1);
                    temp.add(val2);
                    synonymStore.put(val1, temp);
                    synonymStore.put(val2, temp);
                }
            }
        }
        return synonymStore;
    }

    private static Set<String> mergeAndReturn(String val, Set<String> bag) {
        Set<String> temp = new HashSet<>();
        temp.addAll(bag);
        temp.add(val);
        return temp;
    }

    private static Map<String, List<String>> segregateSynonymsVsTestCases(List<String> fileContent, AtomicInteger index) {
        List<String> rawSynonymList = new ArrayList<>();
        List<String> rawTestCaseList = new ArrayList<>();
        Map<String, List<String>> result = new HashMap<>();
        String nextStr = fileContent.get(index.get());
        if (!nextStr.matches(NUMBER_MATCHER)) {
            throw new NumberFormatException("Value at index=" + nextStr + "must be an Integer");
        } else {
            int offset = Integer.parseInt(nextStr) + index.get();
            rawSynonymList.addAll(fileContent.subList(index.get() + 1, offset + 1));
            index.set(offset + 1);
        }

        nextStr = fileContent.get(index.get());
        if (!nextStr.matches(NUMBER_MATCHER)) {
            throw new NumberFormatException("Value at index=" + nextStr + "must be an Integer");
        } else {
            int offset = Integer.parseInt(nextStr) + index.get();
            rawTestCaseList.addAll(fileContent.subList(index.get() + 1, offset + 1));
            index.set(offset + 1);
        }
        result.put(SYNONYM_KEY, rawSynonymList);
        result.put(TEST_CASE_KEY, rawTestCaseList);
        return result;
    }

    private static Map<String, String> populateWithId(List<String> rawList) {
        Map<String, String> temp = new HashMap<>();
        return rawList.stream().collect(Collectors.toMap(s -> s, Function.identity()));
    }

    private static int extractTotalNumberOfTestCases(String firstLine) {
        if (firstLine == null || firstLine.isBlank()) {
            throw new IllegalArgumentException("First line cannot be empty or null");
        } else if (!firstLine.matches(NUMBER_MATCHER)) {
            throw new NumberFormatException("First line must be an Integer");
        } else {
            return Integer.parseInt(firstLine);
        }
    }
}
