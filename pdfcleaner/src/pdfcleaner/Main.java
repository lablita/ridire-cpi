package pdfcleaner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
	public static void main(String[] args) {
		Main m = new Main();

		if (args.length == 0) {
			m.printUsage();
			System.exit(1);
		}
		if (args[0].equals("-h") || args[0].equals("--help")) {
			m.printUsage();
			System.exit(0);
		}
		if (args[0].equals("-s")) // Standard, Single File
		{
			if (args.length != 5) {
				m.printUsage();
				System.exit(1);
			}
			int mwl = Integer.parseInt(args[2]);
			int Msl = Integer.parseInt(args[3]);
			int ptp = Integer.parseInt(args[4]);
			String x = m.run(args[1], mwl, Msl, ptp);
			System.out.println(x);
		} else if (args[0].equals("-t")) // Tuning, Multiple Files
		{
			if (args.length != 12) {
				m.printUsage();
				System.exit(1);
			}
			m.mod_classes0(new File(args[1]));
			m.tuning_multiple_files(args[1], args[2],
					Integer.parseInt(args[3]), Integer.parseInt(args[4]),
					Integer.parseInt(args[5]), Integer.parseInt(args[6]),
					Integer.parseInt(args[7]), Integer.parseInt(args[8]),
					Integer.parseInt(args[9]), Integer.parseInt(args[10]),
					Integer.parseInt(args[11]));
		} else if (args[0].equals("-ts")) // Tuning, Single File
		{
			if (args.length != 12) {
				m.printUsage();
				System.exit(1);
			}
			m.tuning_single_file(args[1], args[2], Integer.parseInt(args[3]),
					Integer.parseInt(args[4]), Integer.parseInt(args[5]),
					Integer.parseInt(args[6]), Integer.parseInt(args[7]),
					Integer.parseInt(args[8]), Integer.parseInt(args[9]),
					Integer.parseInt(args[10]), Integer.parseInt(args[11]));
		} else // Standard, Multiple Files
		{
			if (args.length != 4) {
				m.printUsage();
				System.exit(1);
			}
			int mwl = Integer.parseInt(args[1]);
			int Msl = Integer.parseInt(args[2]);
			int ptp = Integer.parseInt(args[3]);
			m.mod_classes1(args[0]);
			String x = m.run(args[0].substring(args[0]
					.lastIndexOf(File.separator) + 1), args[0].substring(0,
					args[0].lastIndexOf(File.separator)), mwl, Msl, ptp);
			System.out.println(x);
		}
	}

	public String getCleanedText(String args[]) {
		if (args.length == 0) {
			this.printUsage();
			return null;
		}
		if (args[0].equals("-h") || args[0].equals("--help")) {
			this.printUsage();
			return null;
		}
		if (args[0].equals("-s")) // Standard, Single File
		{
			if (args.length != 5) {
				this.printUsage();
				return null;
			}
			int mwl = Integer.parseInt(args[2]);
			int Msl = Integer.parseInt(args[3]);
			int ptp = Integer.parseInt(args[4]);
			String x = this.run(args[1], mwl, Msl, ptp);
			return x;
		} else if (args[0].equals("-t")) // Tuning, Multiple Files
		{
			if (args.length != 12) {
				this.printUsage();
				return null;
			}
			this.mod_classes0(new File(args[1]));
			this.tuning_multiple_files(args[1], args[2], Integer
					.parseInt(args[3]), Integer.parseInt(args[4]), Integer
					.parseInt(args[5]), Integer.parseInt(args[6]), Integer
					.parseInt(args[7]), Integer.parseInt(args[8]), Integer
					.parseInt(args[9]), Integer.parseInt(args[10]), Integer
					.parseInt(args[11]));
		} else if (args[0].equals("-ts")) // Tuning, Single File
		{
			if (args.length != 12) {
				this.printUsage();
				return null;
			}
			this.tuning_single_file(args[1], args[2],
					Integer.parseInt(args[3]), Integer.parseInt(args[4]),
					Integer.parseInt(args[5]), Integer.parseInt(args[6]),
					Integer.parseInt(args[7]), Integer.parseInt(args[8]),
					Integer.parseInt(args[9]), Integer.parseInt(args[10]),
					Integer.parseInt(args[11]));
		} else // Standard, Multiple Files
		{
			if (args.length != 4) {
				this.printUsage();
				return null;
			}
			int mwl = Integer.parseInt(args[1]);
			int Msl = Integer.parseInt(args[2]);
			int ptp = Integer.parseInt(args[3]);
			this.mod_classes1(args[0]);
			String x = this.run(args[0].substring(args[0]
					.lastIndexOf(File.separator) + 1), args[0].substring(0,
					args[0].lastIndexOf(File.separator)), mwl, Msl, ptp);
			return x;
		}
		return null;
	}

	public void mod_classes(String fname, int n) {
		File f;
		FileInputStream fis;
		InputStreamReader isr;
		BufferedReader br;
		try {
			f = new File(fname);
			fis = new FileInputStream(f);
			isr = new InputStreamReader(fis, "8859_1");
			br = new BufferedReader(isr);

			FileOutputStream fos = new FileOutputStream(fname + ".2");
			OutputStreamWriter out = new OutputStreamWriter(fos, "8859_1");

			String x = br.readLine();
			while (x != null) {
				if (x.matches("^.*class=\"ft0[0-9]+0.*$")) {
					br.close();
					out.close();
					File nf = new File(fname + ".2");
					nf.delete();
					return;
				}
				String y = x.replaceAll("\\.ft", ".ft0" + n + "0");
				y = y.replaceAll("class=\"ft", "class=\"ft0" + n + "0");
				out.write(y + "\n");
				x = br.readLine();
			}
			br.close();
			out.close();
		} catch (Exception e) {
		}
		File nf = new File(fname + ".2");
		nf.renameTo(new File(fname));
	}

	public void mod_classes0(File dir) {
		String[] children = dir.list();
		for (int i = 0; i < children.length; i++) {
			String fname = children[i].substring(children[i]
					.lastIndexOf(File.separator) + 1);
			String dirpath = dir.getAbsolutePath() + File.separator + fname;
			fname = fname.substring(0, fname.lastIndexOf(".")) + ".html";
			int pag = 1;
			File f2 = new File(dirpath + File.separator
					+ fname.substring(0, fname.lastIndexOf(".")) + "-" + pag
					+ ".html");
			while (f2.exists()) {
				this.mod_classes(f2.getAbsolutePath(), pag);
				pag++;
				f2 = new File(dirpath + File.separator
						+ fname.substring(0, fname.lastIndexOf(".")) + "-"
						+ pag + ".html");
			}
		}
	}

	public void mod_classes1(String fname) {

		String dir = fname.substring(0, fname.lastIndexOf(File.separator));
		String name = fname.substring(fname.lastIndexOf(File.separator) + 1);
		int pag = 1;
		File f = new File(dir + File.separator
				+ name.substring(0, name.lastIndexOf(".")) + "-" + pag
				+ ".html");
		while (f.exists()) {
			this.mod_classes(f.getAbsolutePath(), pag);
			pag++;
			f = new File(dir + File.separator
					+ name.substring(0, name.lastIndexOf(".")) + "-" + pag
					+ ".html");
		}
	}

	public void printUsage() {
		System.out.println("");
		System.out.println("\tPdfCleaner");
		System.out
				.println("\nPdfCleaner extracts plain text from the output of \"pdftohtml\" Unix tool.\n"
						+ "If you use pdftohtml in the standard mode, it produces many HTML files (one per page) and you must use the \"Multiple file mode\" of PdfClenaer.\n"
						+ "If you use pdftohtml with the -noframes option, it produces only one big HTML file. In this case you must use PdfCleaner in \"Single file mode\".\n"
						+ "With the PdfCleaner tuning mode, you can make several text extractions trying different parameter values: setting a lower bound and an upper bound limit and the step length for any computation, PdfCleaner makes the extraction for any possible combination of parameters.\n");
		System.out.println("Standard Usage:\n");
		System.out
				.println("- Multiple file mode:\tPdfCleaner file_path mwl Msl ptp");
		System.out
				.println("\n\tfile_path: The full path of main (the one with basename) HTML file.");
		System.out.println("\tmwl: the mwl parameter (integer value).");
		System.out.println("\tMsl: the Msl parameter (integer value).");
		System.out.println("\tptp: the ptp parameter (integer value).");
		System.out
				.println("\n- Single file mode:\tPdfCleaner -s file_path mwl Msl ptp");
		System.out.println("\n\tfile_path: the full path of HTML file.");
		System.out.println("\n\nTuning parameters:");
		System.out
				.println("\n- Multiple file mode:\tPdfCleaner -t in_dir out_dir mwl_a mwl_b mwl_p Msl_a Msl_b Msl_p ptp_a ptp_b ptp_p");
		System.out
				.println("\n\tin_dir: the directory path containing a subdirectory for each extracted file.");
		System.out
				.println("\tout_dir: an empty directory where PdfCleaner can save output data.");
		System.out
				.println("\ta lower bound (_a), an upper bound (_b) and the step length (_p) for each parameter.");
		System.out
				.println("\n- Single file mode:\tPdfCleaner -ts in_dir out_dir mwl_a mwl_b mwl_p Msl_a Msl_b Msl_p ptp_a ptp_b ptp_p");
		System.out
				.println("\n\tin_dir: the directory path containing any extracted HTML file.\n");
	}

	// Clean a single file created with the -noframes option of pdftohtml
	public String run(String fname, int minWordLength, int maxShortLines,
			int ptpWords) {
		// System.out.println("Processing... " + fname);
		HtmlComplexParser h = new HtmlComplexParser(fname, minWordLength,
				maxShortLines);
		CssClass c = h.getBestClass();
		// System.out.println("Classe: " + c.font_family + " - " + c.font_size);
		Filter f;
		String good = h.wl.toString(c);
		f = new BrokenWordsFilter(good);
		f.apply();
		good = f.toString();

		f = new BadWordsFilter(good);
		f.apply();
		good = f.toString();

		f = new PointToPointFilter(good, ptpWords);
		f.apply();
		good = f.toString();
		f = new SingleNumberFilter(good);
		f.apply();
		good = f.toString();

		f = new BibliographyFilter(good);
		f.apply();
		good = f.toString();

		return good;
	}

	// Clean the html files (one per page) created with pdftohtml
	public String run(String fname, String dirpath, int minWordLength,
			int maxShortLines, int ptpWords) {
		// System.out.println("Processing... " + dirpath + File.separator +
		// fname);
		HtmlComplexParser h = new HtmlComplexParser(fname, dirpath,
				minWordLength, maxShortLines);
		CssClass c = h.getBestClass();
		// System.out.println("Classe: " + c.font_family + " - " + c.font_size);
		Filter f;
		String good = h.wl.toString(c);
		f = new BrokenWordsFilter(good);
		f.apply();
		good = f.toString();
		f = new BadWordsFilter(good);
		f.apply();
		good = f.toString();
		f = new PointToPointFilter(good, ptpWords);
		f.apply();
		good = f.toString();
		f = new SingleNumberFilter(good);
		f.apply();
		good = f.toString();
		f = new BibliographyFilter(good);
		f.apply();
		good = f.toString();
		return good;
	}

	public void tuning_multiple_files(String in_dir, String out_dir, int mwl_a,
			int mwl_b, int mwl_p, int Msl_a, int Msl_b, int Msl_p, int ptp_a,
			int ptp_b, int ptp_p) {
		Writer output;
		File dir = new File(in_dir);
		String outDir = out_dir;
		int count = 1;

		for (int ii = ptp_a; ii <= ptp_b; ii += ptp_p) // ptpwords
		{
			for (int j = Msl_a; j <= Msl_b; j += Msl_p) // Msl
			{
				for (int k = mwl_a; k <= mwl_b; k += mwl_p) // mwl
				{
					System.out.println("\n--->\t" + k + " - " + j + " - " + ii
							+ "\t[Prova n. " + count + "]\n");
					count++;
					File f = new File(outDir + File.separator + k + "_" + j
							+ "_" + ii);
					if (f.mkdir()) {
						String[] children = dir.list();
						for (int i = 0; i < children.length; i++) {
							String fname = children[i].substring(children[i]
									.lastIndexOf(File.separator) + 1);
							String dirpath = dir.getAbsolutePath()
									+ File.separator + fname;
							fname = fname.substring(0, fname.lastIndexOf("."))
									+ ".html";
							System.out.println(fname);

							String x = this.run(fname, dirpath, k, j, ii);
							try {
								output = new BufferedWriter(new FileWriter(
										new File(f.getAbsolutePath()
												+ File.separator
												+ fname.substring(0, fname
														.lastIndexOf("."))
												+ ".txt")));
								output.write(x);
								output.close();
							} catch (IOException ex) {
								Logger.getLogger(Main.class.getName()).log(
										Level.SEVERE, null, ex);
							}
						}
					}
				}
			}
		}
	}

	public void tuning_single_file(String in_dir, String out_dir, int mwl_a,
			int mwl_b, int mwl_p, int Msl_a, int Msl_b, int Msl_p, int ptp_a,
			int ptp_b, int ptp_p) {
		Writer output;
		File dir = new File(in_dir);
		String outDir = out_dir;
		int count = 1;

		for (int ii = ptp_a; ii <= ptp_b; ii += ptp_p) // ptpwords
		{
			for (int j = Msl_a; j <= Msl_b; j += Msl_p) // Msl
			{
				for (int k = mwl_a; k <= mwl_b; k += mwl_p) // mwl
				{
					System.out.println("\n--->\t" + k + " - " + j + " - " + ii
							+ "\t[Prova n. " + count + "]\n");
					count++;
					File f = new File(outDir + File.separator + k + "_" + j
							+ "_" + ii);
					if (f.mkdir()) {
						// Writer output;
						String[] children = dir.list();
						for (int i = 0; i < children.length; i++) {
							String fname = children[i].substring(children[i]
									.lastIndexOf(File.separator) + 1);
							System.out.println(fname);
							String x = this.run(dir.getAbsolutePath()
									+ File.separator + children[i], k, j, ii);

							try {
								output = new BufferedWriter(
										new FileWriter(
												new File(
														outDir
																+ File.separator
																+ k
																+ "_"
																+ j
																+ "_"
																+ ii
																+ File.separator
																+ children[i]
																		.substring(
																				0,
																				children[i]
																						.lastIndexOf("."))
																+ ".txt")));
								output.write(x);
								output.close();
							} catch (IOException ex) {
								Logger.getLogger(Main.class.getName()).log(
										Level.SEVERE, null, ex);
							}
						}
					}
				}
			}
		}
	}
}
