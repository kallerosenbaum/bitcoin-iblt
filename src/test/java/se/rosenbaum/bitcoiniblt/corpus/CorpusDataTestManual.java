package se.rosenbaum.bitcoiniblt.corpus;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.junit.Before;
import org.junit.Test;
import se.rosenbaum.bitcoiniblt.BlockStatsClientCoderTest;
import se.rosenbaum.bitcoiniblt.printer.CellCountVSFailureProbabilityPrinter;
import se.rosenbaum.bitcoiniblt.printer.FailureProbabilityPrinter;
import se.rosenbaum.bitcoiniblt.printer.IBLTSizeBlockStatsPrinter;
import se.rosenbaum.bitcoiniblt.printer.IBLTSizeVsFailureProbabilityPrinter;
import se.rosenbaum.bitcoiniblt.printer.ValueSizeCellCountPrinter;
import se.rosenbaum.bitcoiniblt.util.AggregateResultStats;
import se.rosenbaum.bitcoiniblt.util.BlockStatsResult;
import se.rosenbaum.bitcoiniblt.util.Interval;
import se.rosenbaum.bitcoiniblt.util.TestConfig;
import se.rosenbaum.bitcoiniblt.util.TransactionSets;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Orphan blocks: 352802
// 352802 A fork. There are two of these one with 596 tx and one (the orphaned) with 637 tx.
public class CorpusDataTestManual extends BlockStatsClientCoderTest implements TransactionStore
{

	private CorpusData corpusStats;
	private File testFileDir;
	private File testResultDir;

	@Before
	public void setup()
	{
		String corpusHomePath = testProps.getProperty("corpus.directory");

		this.corpusStats = new CorpusData(new File(corpusHomePath));
		MAINNET_BLOCK = CorpusData.HIGHEST_BLOCK_HASH;
		try
		{
			corpusStats.calculateStatistics();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		blockCount = corpusStats.blockCount;

		testFileDir = new File(tempDirectory, "corpustestfiles");
		testFileDir.mkdirs();
		testResultDir = new File(tempDirectory, "corpustestresults");
		testResultDir.mkdirs();
	}

	@Test
	public void testFactor1() throws IOException
	{
		int factor = 1;
		int sampleCount = 1000;

		int extras = (int) Math.ceil(corpusStats.averageExtrasPerBlock) * factor;

		FailureProbabilityPrinter printer = new IBLTSizeVsFailureProbabilityPrinter(tempDirectory);

		CorpusDataTestConfig testConfig = new CorpusDataTestConfig(extras, extras, 100002);

		Interval interval = new Interval(0, testConfig.getCellCount());
		while (true)
		{
			AggregateResultStats result = testFailureProbability(printer, testConfig, sampleCount);

			if (result.getFailureProbability() > 0.02 && result.getFailureProbability() < 0.1)
			{
				printer.addResult(testConfig, result);
			}

			if (result.getFailureProbability() < 0.05)
			{
				interval.setHigh(testConfig.getCellCount());
			}
			else
			{
				interval.setLow(testConfig.getCellCount());
			}
			testConfig = new CorpusDataTestConfig(extras, extras, interval.nextValue(testConfig));

			if (!interval.isInsideInterval(testConfig.getCellCount()))
			{
				break;
			}
		}

		printer.finish();
	}

	@Test
	public void testFromTestFiles() throws Exception
	{
		int cellCount = 300;

		TestConfigGenerator configGenerator = null;
		for (int factor : new int[] { 1, 10, 100, 1000 })
		{
			FailureProbabilityPrinter printer = new IBLTSizeVsFailureProbabilityPrinter(tempDirectory);
			if (factor > 1)
			{
				cellCount = configGenerator.getCellCount() * 9;
			}
			configGenerator = new TestFileTestConfigGenerator(getFile(factor), 3, 8, 64, 4, cellCount, this);

			configGenerator = calculateSizeFromTargetProbability(printer, getFile(factor), configGenerator, factor, 0.05);
		}
	}

	@Test
	public void testFromTestFile() throws Exception
	{
		int cellCount = 6000;
		int factor = 100;
		TestFileTestConfigGenerator configGenerator;

		FailureProbabilityPrinter printer = new IBLTSizeVsFailureProbabilityPrinter(tempDirectory);
		configGenerator = new TestFileTestConfigGenerator(getFile(factor), 3, 8, 64, 4, cellCount, this);

		calculateSizeFromTargetProbability(printer, getFile(factor), configGenerator, factor, 0.05);
	}

	@Test
	public void testValueSizeFor5PercentFailureProbabilityFromRealDataFileMultipleTimes() throws Exception
	{
		for (int i = 0; i < 3; i++)
		{
			testValueSizeFor5PercentFailureProbabilityFromRealDataFile();
		}
	}

	@Test
	public void testValueSizeFor5PercentFailureProbabilityFromRealDataFile() throws Exception
	{
		int cellCount = 32385;
		File testFile = new File(testFileDir, "test-real.txt");
		TestConfigGenerator configGenerator = new TestFileTestConfigGenerator(testFile, 3, 8, 64, 4, cellCount, this);

		int[] category = new int[] { 8, 16, 32, 48, 64, 80, 96, 112, 128, 144, 160, 176, 192, 208, 256, 270, 280, 512 };
		IBLTSizeBlockStatsPrinter valueSizeCellCountPrinter = new ValueSizeCellCountPrinter(tempDirectory, category.length,
				"IBLT size for 5% failure probability, corpus-au");

		for (int i = 0; i < category.length; i++)
		{
			FailureProbabilityPrinter failureProbabilityPrinter = new CellCountVSFailureProbabilityPrinter(tempDirectory);
			configGenerator.setValueSize(category[i]);

			Interval interval = new Interval(0, configGenerator.getCellCount());
			AggregateResultStats closestResult = null;
			TestConfigGenerator closestTestConfig = null;
			while (true)
			{
				AggregateResultStats result = testFailureProbabilityForConfigGenerator(failureProbabilityPrinter, configGenerator);
				failureProbabilityPrinter.addResult(configGenerator, result);
				if (result.getFailureProbability() <= 0.05)
				{
					if (result.getFailureProbability() == 0.05)
					{
						interval.setLow(configGenerator.getCellCount());
					}
					interval.setHigh(configGenerator.getCellCount());
					closestResult = result;
					closestTestConfig = configGenerator;
				}
				else
				{
					interval.setLow(configGenerator.getCellCount());
				}
				configGenerator = configGenerator.cloneGenerator();
				configGenerator.setCellCount(interval.nextValue(configGenerator));

				if (!interval.isInsideInterval(configGenerator.getCellCount()))
				{
					configGenerator.setCellCount(interval.getHigh() * 2);
					break;
				}
			}
			failureProbabilityPrinter.finish();
			valueSizeCellCountPrinter.addResult(closestTestConfig, closestResult);
		}
		valueSizeCellCountPrinter.finish();
	}

	@Test
	public void testFromRealDataFile() throws Exception
	{
		int cellCount = 4200;
		File testFile = new File(testFileDir, "test-real.txt");

		TestFileTestConfigGenerator configGenerator = null;
		for (double targetProbability : new double[] { 0.04, 0.05, 0.06 })
		{
			FailureProbabilityPrinter printer = new IBLTSizeVsFailureProbabilityPrinter(tempDirectory);
			configGenerator = new TestFileTestConfigGenerator(testFile, 3, 8, 64, 4, configGenerator == null ? cellCount
					: configGenerator.getCellCount(), this);

			calculateSizeFromTargetProbability(printer, testFile, configGenerator, -1, targetProbability);
		}
	}


	protected TestConfigGenerator calculateSizeFromTargetProbability(FailureProbabilityPrinter printer, File testFile,
			TestConfigGenerator configGenerator, int factor, double targetProbability) throws Exception
	{
		Interval interval = new Interval(0, configGenerator.getCellCount());
		AggregateResultStats closestResult = null;
		TestConfigGenerator closestTestConfig = null;
		while (true)
		{
			AggregateResultStats result = testFailureProbabilityForConfigGenerator(printer, configGenerator);
			printer.addResult(configGenerator, result);
			if (result.getFailureProbability() <= targetProbability)
			{
				if (result.getFailureProbability() == targetProbability)
				{
					interval.setLow(configGenerator.getCellCount());
				}
				interval.setHigh(configGenerator.getCellCount());
				closestResult = result;
				closestTestConfig = configGenerator;
			}
			else
			{
				interval.setLow(configGenerator.getCellCount());
			}
			configGenerator = configGenerator.cloneGenerator();
			configGenerator.setCellCount(interval.nextValue(configGenerator));

			if (!interval.isInsideInterval(configGenerator.getCellCount()))
			{
				configGenerator.setCellCount(interval.getHigh());
				break;
			}
		}
		if (factor == -1)
		{
			printTestResultFile(closestTestConfig, closestResult, testFile, targetProbability);
		}
		else
		{
			printTestResultFile(closestTestConfig, closestResult, factor, testFile, targetProbability);
		}
		printer.finish();
		return closestTestConfig;
	}

	@Test
	public void testGenerateTestFileFactor1_10_100_1000() throws IOException
	{
		int sampleCount = 1000;

		for (int i = 0; i < 4; i++)
		{
			createTestFile(sampleCount, i);
		}
	}

	@Test
	public void testFindPercentilesOfExtras() throws IOException
	{
		int averageExtras = (int) Math.ceil(corpusStats.averageExtrasPerBlock);
		int countBelowEqualAverageExtras = 0;
		int countMoreThanAverageExtras = 0;
		List<Integer> unknowns = new ArrayList<Integer>();
		for (CorpusData.Node node : CorpusData.Node.values())
		{
			AverageExtrasPercentile handler = new AverageExtrasPercentile();
			corpusStats.getStats(node, handler);
			blockCount += handler.blocks.size();

			for (IntPair intPair : handler.blocks.values())
			{
				unknowns.add(intPair.unknowns);
				if (intPair.unknowns <= averageExtras)
				{
					countBelowEqualAverageExtras++;
				}
				else
				{
					countMoreThanAverageExtras++;
				}
			}
		}

		// processBlocks(CorpusData.HIGHEST_BLOCK_HASH, 720, )
		/*
		 * 1. Collect all extras (not coinbases) for all blocks and nodes from corpus. Calculate the average extras, E,
		 * over the remaining. 2. Calculate the average tx rate, R, over the corpus. Sum the number of transactions in
		 * all blocks and divide it with the data collection period in seconds. 3. Now calculate the
		 * "extras per tx rate", E/R. 4. Absents, A, is calculated from E and the ratio extras/absent 5. Assume that E/R
		 * is constant and that the extras/absent ratio holds for all tx rates.
		 */

		System.out.println("Assumed extras/absents: 1/1");

		System.out.println("Number of AU blocks: " + corpusStats.blockCount);
		System.out.println("Lowest/highes block: " + corpusStats.lowestBlock + "/" + corpusStats.highestBlock);
		System.out.println("Exact avg extras   : " + corpusStats.averageExtrasPerBlock);
		System.out.println("Ceil of extras, E  : " + averageExtras);
		System.out.println("Avg tx rate, R     : " + corpusStats.txRate);
		System.out.println("Avg E/R            : " + corpusStats.extrasPerTxRate);

		System.out.println("Count <= E         : " + countBelowEqualAverageExtras);
		System.out.println("Count >  E         : " + countMoreThanAverageExtras);
		System.out
				.println("Percentage below   : " + 100 * countBelowEqualAverageExtras / (countBelowEqualAverageExtras + countMoreThanAverageExtras));

		Collections.sort(unknowns);
		System.out.println("Rough percentiles:");
		int size = unknowns.size();
		for (int i = 1; i <= 9; i++)
		{
			int percent = 10 * i;
			System.out.println(percent + "% has <= " + unknowns.get(size * i / 10 - 1) + " extras");
		}
		for (int i = 1; i <= 10; i++)
		{
			int percent = 90 + i;
			System.out.println(percent + "% has <= " + unknowns.get(size * (90 + i) / 100 - 1) + " extras");
		}
	}

	@Test
	public void createTestFileMimicCorpus() throws IOException
	{
		FileWriter fileWriter = new FileWriter(new File(testFileDir, "test-real.txt"));
		final TestFilePrinter testFilePrinter = new TestFilePrinter(fileWriter);

		final List<String> extra = new ArrayList<String>();
		final List<String> absent = new ArrayList<String>();

		corpusStats.getStats(CorpusData.Node.AU, new CorpusData.RecordHandler()
		{
			boolean firstBlock = true;

			public void handle(Record record)
			{
				if (record.type == Type.COINBASE)
				{
					if (!firstBlock)
					{
						while (absent.size() < extra.size())
						{
							List<Transaction> randomTransactions = getRandomTransactions(extra.size() - absent.size(), false);
							for (Transaction randomTransaction : randomTransactions)
							{
								String hashAsString = randomTransaction.getHashAsString();
								if (!absent.contains(hashAsString))
								{
									absent.add(hashAsString);
								}
							}
						}
						testFilePrinter.writeTransactions(extra, absent);
						extra.clear();
						absent.clear();
					}
					else
					{
						firstBlock = false;
					}
				}
				else if (record.type == Type.UNKNOWN)
				{
					extra.add(new Sha256Hash(record.txid).toString());
				}
				else if (record.type == Type.MEMPOOL_ONLY)
				{
					if (absent.size() < extra.size())
					{
						// Fill up with made up absent transactions, by taking equally many from MEMPOOL_ONLY
						// as there are extra.
						Sha256Hash sha256Hash = new Sha256Hash(record.txid);
						try
						{
							if (getTransaction(sha256Hash) != null)
							{
								absent.add(sha256Hash.toString());
							}
						}
						catch (IOException e)
						{
							throw new RuntimeException(e);
						}
					}
				}
			}
		});

		testFilePrinter.writeTransactions(extra, absent);
		fileWriter.close();
	}

	private int averageBlockSize()
	{
		int totalBlockSize = 0;
		Block block = getBlock(new Sha256Hash(MAINNET_BLOCK));
		for (int i = 1; i < corpusStats.blockCount; i++)
		{
			totalBlockSize += block.getOptimalEncodingMessageSize();
			block = getBlock(block.getPrevBlockHash());
		}
		return totalBlockSize / corpusStats.blockCount;
	}

	private void printTestResultFile(TestConfig testConfig, AggregateResultStats result, File inputFile, double targetProbability) throws IOException
	{
		File resultFile = getResultFile("test-result-real");
		PrintWriter out = new PrintWriter(new FileWriter(resultFile));
		out.println("Input file                    : " + inputFile.getName());
		out.println("Average block size [Bytes]    : " + averageBlockSize());
		out.println("Average tx count per block    : " + (corpusStats.txCount / corpusStats.blockCount));
		printCommon(testConfig, result, out, targetProbability);
		out.flush();
		out.close();
	}

	private void printTestResultFile(TestConfig testConfig, AggregateResultStats result, int factor, File inputFile, double targetProbability)
			throws IOException
	{
		File resultFile = getResultFile("test-result-factor-" + factor);
		PrintWriter out = new PrintWriter(new FileWriter(resultFile));
		out.println("Input file                    : " + inputFile.getName());
		out.println("Estimated block size [Bytes]  : " + averageBlockSize() * factor);
		out.println("Estimated tx count per block  : " + (corpusStats.txCount / corpusStats.blockCount) * factor);
		out.println("Extra tx                      : " + testConfig.getExtraTxCount());
		out.println("Absent tx                     : " + testConfig.getAbsentTxCount());
		printCommon(testConfig, result, out, targetProbability);
		out.flush();
		out.close();
	}

	private void printCommon(TestConfig testConfig, AggregateResultStats result, PrintWriter out, double targetProbability)
	{
		out.println("Sample count                  : " + (result.getFailures() + result.getSuccesses()));
		out.println("Target failure probability    : " + targetProbability);
		out.println("IBLT size                     : " + testConfig.getIbltSize());
		out.println("Cell count                    : " + testConfig.getCellCount());
		out.println("Hash functions                : " + testConfig.getHashFunctionCount());
		out.println("Key size                      : " + testConfig.getKeySize());
		out.println("Value size                    : " + testConfig.getValueSize());
		out.println("KeyHashSize                   : " + testConfig.getKeyHashSize());
	}

	private File getResultFile(String prefix)
	{
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
		return new File(testResultDir, prefix + "-" + dateFormat.format(new Date()) + ".txt");
	}

	private static class DefaultTestListener implements TestListener {
		AggregateResultStats stats = new AggregateResultStats();
		int i = 1;
		FailureProbabilityPrinter printer;

		public DefaultTestListener(FailureProbabilityPrinter printer) {
			this.printer = printer;
		}

		public void testPerformed(TestConfig config, BlockStatsResult result) {
			stats.addSample(result);
			if (i % 100 == 0)
			{
				printer.logResult(config, stats);
			}
			i++;
		}

		public AggregateResultStats getStats() {
			return stats;
		}
	}

	protected AggregateResultStats testFailureProbabilityForConfigGenerator(FailureProbabilityPrinter printer, TestConfigGenerator configGenerator)
			throws Exception {
		DefaultTestListener listener = new DefaultTestListener(printer);
		testFailureProbabilityForConfigGenerator(configGenerator, listener);
		return listener.getStats();
	}

	protected void testFailureProbabilityForConfigGenerator(TestConfigGenerator configGenerator, TestListener testListener)
			throws Exception
	{
		TestConfig config = configGenerator.createNextTestConfig();
		while (config != null)
		{
			BlockStatsResult result = testBlockStats(config);
			if (testListener != null) {
				testListener.testPerformed(config, result);
			}
			config = configGenerator.createNextTestConfig();
		}
	}

	private static class IntPair
	{
		int unknowns = 0;
		int knowns = 0;
	}

	private static class AverageExtrasPercentile implements CorpusData.RecordHandler
	{
		Map<Integer, IntPair> blocks = new HashMap<Integer, IntPair>();
		IntPair currentBlock = new IntPair();
		int currentHeight = 0;

		public void handle(Record record)
		{
			if (record.type == Type.COINBASE)
			{
				blocks.put(currentHeight, currentBlock);
				currentBlock = new IntPair();
				currentHeight = record.blockNumber;
			}
			else if (record.type == Type.UNKNOWN)
			{
				currentBlock.unknowns++;
			}
			else if (record.type == Type.KNOWN)
			{
				currentBlock.knowns++;
			}
		}
	}

	private void createTestFile(long sampleCount, long factorExponent) throws IOException
	{
		int factor = (int) Math.pow(10, factorExponent);
		int extras = (int) Math.ceil(corpusStats.averageExtrasPerBlock) * factor;
		CorpusDataTestConfig testConfig = new CorpusDataTestConfig(extras, extras, 100002);
		File file = getFile(factor);

		FileWriter fileWriter = new FileWriter(file);
		TestFilePrinter testFilePrinter = new TestFilePrinter(fileWriter);

		for (int i = 0; i < sampleCount; i++)
		{
			testFilePrinter.printTransactionSets(testConfig.createTransactionSets());
		}

		fileWriter.close();
	}

	private File getFile(long factor)
	{
		return new File(testFileDir, "test-factor-" + factor + ".txt");
	}

	private class CorpusDataTestConfig extends TestConfig
	{

		public CorpusDataTestConfig(int extraTxCount, int absentTxCount, int cellCount)
		{
			super(0, extraTxCount, absentTxCount, 3, 8, 64, 4, cellCount);
		}

		@Override
		public TransactionSets createTransactionSets()
		{
			List<Transaction> randomTransactions = getRandomTransactions(getExtraTxCount() + getAbsentTxCount(), false);
			TransactionSets transactionSets = new TransactionSets();
			// As with most other tests, we just care about differences. Transactions that are both in sender's and
			// receiver's transacitons will just be added and deleted so they don't affect the result.
			transactionSets.setSendersTransactions(randomTransactions.subList(0, getExtraTxCount()));
			transactionSets.setReceiversTransactions(randomTransactions.subList(getExtraTxCount(), getExtraTxCount() + getAbsentTxCount()));
			return transactionSets;
		}
	}

	@Test
	public void testPrintBlocksReceivedAtAU() throws IOException {
		RecordInputStream recordInputStream = corpusStats.getRecordInputStream(CorpusData.Node.SF);
		Record record = recordInputStream.readRecord();
		while (record != null) {
			if (record.type == Type.COINBASE) {
				System.out.println(new Date(record.timestamp*1000) + " " + record.blockNumber);
			}
			record = recordInputStream.readRecord();
		}
		recordInputStream.close();
	}
}
