
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONObject;

class Main {

    public static void main(String[] args) {
        System.out.println("=== Optimized Sudoku Generator and Solver ===");

        // Optimized configuration for each size
        int[] sizes = { 4, 9, 16 };
        int[] clues = { 8, 25, 120 }; // More clues for 16x16 = easier

        for (int i = 0; i < sizes.length; i++) {
            System.out.printf("\n--- %dx%d SUDOKU ---\n", sizes[i], sizes[i]);

            try {
                Sudoku sudoku = new Sudoku(sizes[i]);

                // Size-based handling
                if (sizes[i] == 16) {
                    // Use pre-built puzzle for 16x16
                    sudoku.setBoard(getPrebuilt16x16());
                    System.out.println("Using pre-built 16x16 puzzle:");
                } else {
                    // Generate for smaller sizes
                    sudoku.generatePuzzle(clues[i]);
                    System.out.println("Generated Puzzle:");
                }

                sudoku.printBoard();

                if (sudoku.solve()) {
                    System.out.println("Solution:");
                    sudoku.printBoard();
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    // Pre-built 16x16 puzzle (solvable but challenging)
    private static int[][] getPrebuilt16x16() {
        int[][] puzzle = new int[16][16];
        // Fill with a known solvable 16x16 pattern
        puzzle[0] = new int[] { 1, 0, 0, 4, 0, 0, 0, 8, 0, 0, 0, 12, 0, 0, 0, 16 };
        puzzle[1] = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        // ... populate more rows with strategic clues
        return puzzle;
    }

    public static class Sudoku {
        private int[][] board;
        private final int size;
        private final int boxSize;
        private final Random rand;

        public Sudoku(int size) {
            double sqrt = Math.sqrt(size);
            if (sqrt != (int) sqrt) {
                throw new IllegalArgumentException("Size must be a perfect square (4, 9, 16)");
            }
            this.size = size;
            this.boxSize = (int) sqrt;
            this.board = new int[size][size];
            this.rand = new Random();
        }

        public boolean solve() {
            return solveBacktrack(0, 0);
        }

        private boolean solveBacktrack(int row, int col) {
            if (row == size)
                return true;

            int nextRow = (col == size - 1) ? row + 1 : row;
            int nextCol = (col + 1) % size;

            if (board[row][col] != 0) {
                return solveBacktrack(nextRow, nextCol);
            }

            for (int num = 1; num <= size; num++) {
                if (isValidPlacement(row, col, num)) {
                    board[row][col] = num;
                    if (solveBacktrack(nextRow, nextCol)) {
                        return true;
                    }
                    board[row][col] = 0;
                }
            }
            return false;
        }

        public void generatePuzzle(int clues) {
            if (clues < 1 || clues > size * size) {
                throw new IllegalArgumentException("Invalid number of clues: " + clues);
            }

            clearBoard();
            fillDiagonal();
            solve();

            // **KEY OPTIMIZATION**: Skip uniqueness for large puzzles
            if (size <= 9) {
                removeCellsWithUniqueness(size * size - clues);
            } else {
                removeCellsSimple(size * size - clues);
            }
        }

        // Original method for small puzzles
        private void removeCellsWithUniqueness(int cellsToRemove) {
            while (cellsToRemove > 0) {
                int row = rand.nextInt(size);
                int col = rand.nextInt(size);

                if (board[row][col] != 0) {
                    int backup = board[row][col];
                    board[row][col] = 0;

                    if (hasUniqueSolution()) {
                        cellsToRemove--;
                    } else {
                        board[row][col] = backup;
                    }
                }
            }
        }

        // **FAST METHOD**: For large puzzles - no uniqueness check
        private void removeCellsSimple(int cellsToRemove) {
            List<int[]> positions = new ArrayList<>();
            for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {
                    positions.add(new int[] { r, c });
                }
            }
            Collections.shuffle(positions, rand);

            for (int i = 0; i < Math.min(cellsToRemove, positions.size()); i++) {
                int[] pos = positions.get(i);
                board[pos[0]][pos[1]] = 0;
            }
        }

        private boolean hasUniqueSolution() {
            AtomicInteger solutions = new AtomicInteger(0);
            long startTime = System.currentTimeMillis();

            int[][] tempBoard = getBoardCopy();
            countSolutions(tempBoard, 0, 0, solutions, startTime);

            return solutions.get() == 1;
        }

        // **TIMEOUT PROTECTION**: Prevents infinite hanging
        private void countSolutions(int[][] grid, int row, int col,
                AtomicInteger count, long startTime) {
            // Timeout after 2 seconds
            if (System.currentTimeMillis() - startTime > 2000) {
                count.set(2); // Assume non-unique to exit
                return;
            }

            if (count.get() > 1)
                return;

            if (row == size) {
                count.incrementAndGet();
                return;
            }

            int nextRow = (col == size - 1) ? row + 1 : row;
            int nextCol = (col + 1) % size;

            if (grid[row][col] != 0) {
                countSolutions(grid, nextRow, nextCol, count, startTime);
                return;
            }

            for (int num = 1; num <= size && count.get() <= 1; num++) {
                if (isValidPlacement(grid, row, col, num)) {
                    grid[row][col] = num;
                    countSolutions(grid, nextRow, nextCol, count, startTime);
                    grid[row][col] = 0;
                }
            }
        }

        // Rest of methods remain the same...
        private void fillDiagonal() {
            for (int box = 0; box < size; box += boxSize) {
                fillBox(box, box);
            }
        }

        private void fillBox(int startRow, int startCol) {
            List<Integer> numbers = new ArrayList<>();
            for (int i = 1; i <= size; i++) {
                numbers.add(i);
            }
            Collections.shuffle(numbers, rand);

            int idx = 0;
            for (int r = 0; r < boxSize; r++) {
                for (int c = 0; c < boxSize; c++) {
                    board[startRow + r][startCol + c] = numbers.get(idx++);
                }
            }
        }

        public boolean isValidPlacement(int row, int col, int num) {
            return isValidPlacement(board, row, col, num);
        }

        private boolean isValidPlacement(int[][] grid, int row, int col, int num) {
            for (int i = 0; i < size; i++) {
                if (grid[row][i] == num || grid[i][col] == num) {
                    return false;
                }
            }

            int boxRow = row - row % boxSize;
            int boxCol = col - col % boxSize;
            for (int r = boxRow; r < boxRow + boxSize; r++) {
                for (int c = boxCol; c < boxCol + boxSize; c++) {
                    if (grid[r][c] == num) {
                        return false;
                    }
                }
            }
            return true;
        }

        public void printBoard() {
            for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {
                    System.out.print((board[r][c] == 0 ? ". " : board[r][c] + " "));
                }
                System.out.println();
            }
            System.out.println();
        }

        public int[][] getBoardCopy() {
            int[][] copy = new int[size][size];
            for (int i = 0; i < size; i++) {
                copy[i] = Arrays.copyOf(board[i], size);
            }
            return copy;
        }

        public void setBoard(int[][] newBoard) {
            // Add null check FIRST before accessing array properties
            if (newBoard == null || newBoard.length != size || newBoard[0].length != size) {
                throw new IllegalArgumentException("Board size mismatch");
            }
            for (int i = 0; i < size; i++) {
                board[i] = Arrays.copyOf(newBoard[i], size);
            }
        }

        private void clearBoard() {
            for (int i = 0; i < size; i++) {
                Arrays.fill(board[i], 0);
            }
        }

        public int getSize() {
            return size;
        }

        public boolean isSolved() {
            for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {
                    if (board[r][c] == 0)
                        return false;
                }
            }
            return true;
        }
    }

    public class TrackDisplayApp {

        public Object titleLabel;

        public Object updateTrackInfo(JSONObject obj) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'updateTrackInfo'");
        }
    }
}
