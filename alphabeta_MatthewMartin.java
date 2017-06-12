import java.util.LinkedList;
import java.util.Queue;

public class alphabeta_MatthewMartin extends AIModule
{
	// The player we are playing as (1 or 2).
	private int ourPlayer;
	
	public class TreeNode
	{
		// The current game state.
		public GameStateModule state;
		// The node's children.
		public TreeNode[] children;
		// The parent node.
		public TreeNode parent;
		// The current best found Nash Equilibrium.
		public int v;
		// Which child produced the current v value.
		public int vFrom;
		// This node's evaluation value (estimation of Nash Equlilbrium).
		public int eval;
		// True iff this is a max node.
		public boolean max;
		// The tree level of this node.
		public int level;
		// The current best move found.
		public int move;
		// The lowest level expanded.
		public int lowestLevel;
		
		// Alpha/beta values. Used for alpha-beta pruning.
		public int alpha;
		public int beta;
		
		// Generic constructor.
		public TreeNode(GameStateModule _state, TreeNode _parent)
		{
			children = null;
			state = _state;
			parent = _parent;
			v = 0;
			vFrom = -99;
			eval = 0;
			max = false;
			level = 0;
			move = -99;
			lowestLevel = 0;
			
			alpha = Integer.MIN_VALUE;
			beta = Integer.MAX_VALUE;
		}
	}
	
	// Sets the next move to be made by the AI (by setting chosenMove).
	@Override
	public void getNextMove(final GameStateModule state)
	{
		ourPlayer = state.getActivePlayer();
		TreeNode root;
		
		int currLevel = 4;
		while (true)
		{
			// Build the game tree from the current base state.
			root = new TreeNode(state, null);
			root.eval = eval(root.state);
			root.max = true;
			root.level = 0;
			
			// If we run out of time at any point, exit (the last chosen value will be used).
			if (terminate) break;
			expandToLevel(root, currLevel, Integer.MIN_VALUE, Integer.MAX_VALUE);
			if (terminate) break;
			int newMove = getMove(root);
			if (terminate) break;
			chosenMove = newMove;
			
			// If attempted to expand n layers, but only expanded n-1 layers, then we have already expanded to the bottom.
			// Break to avoid attempting to expand huge numbers of layers.
			if (currLevel > root.lowestLevel) break;
			
			currLevel++;
		}
	}
	
	// After expanding the tree, chooses the best known move based on their evaluation values.
	public int getMove(TreeNode root)
	{
		int bestV = root.v;
		
		int currMove = -99;
		int currEval = Integer.MIN_VALUE;
		// For each possible connect four move.
		for (int i = 0; i < 7; i++)
		{
			if (terminate) return -1;
			if (root.children[i] == null) break;
			
			if (root.children[i].v == bestV)
			{
				if (root.children[i].eval > currEval)
				{
					currEval = root.children[i].eval;
					currMove = root.children[i].move;
				}
			}
		}
		return currMove;
	}
	
	// Expands the node to the passed level, using alpha-beta pruning.
	public void expandToLevel(TreeNode node, int level, int alpha, int beta)
	{
		if (terminate) return;
		if (node.state.isGameOver() || node.level == level)
		{
			node.v = node.eval;
			return;
		}
		
		node.v = (node.max ? Integer.MIN_VALUE : Integer.MAX_VALUE);
		node.children = getMovesEvalOrdered(node);
		if (terminate) return;
		for (int i = 0; i < 7; i++)
		{
			if (node.children[i] == null) break;
			
			expandToLevel(node.children[i], level, alpha, beta);
			if (terminate) return;
			
			if (node.lowestLevel < node.children[i].lowestLevel) node.lowestLevel = node.children[i].lowestLevel;
			
			if (node.max)
			{
				if (node.v < node.children[i].v)
				{
					node.v = node.children[i].v;
				}
				if (node.v >= beta)
				{
					break;
				}
				if (alpha < node.v)
				{
					alpha = node.v;
				}
			}
			else
			{
				if (node.v > node.children[i].v)
				{
					node.v = node.children[i].v;
				}
				if (node.v <= alpha)
				{
					break;
				}
				if (beta > node.v)
				{
					beta = node.v;
				}
			}
		}
	}
	
	// Returns an array of possible nodes that can be arrived at from the passed node (unsorted).
	public TreeNode[] getMovesUnordered(TreeNode node)
	{
		
		TreeNode[] result = new TreeNode[7];
		TreeNode newNode;
		GameStateModule newState;
		int i = 0;
		for (int x = 0; x < 7; x++)
		{
			if (terminate) return null;
			if (node.state.canMakeMove(x))
			{
				newState = node.state.copy();
				newState.makeMove(x);
				
				newNode = new TreeNode(newState, node);
				newNode.max = !node.max;
				newNode.lowestLevel = newNode.level = node.level + 1;
				newNode.move = x;
				
				result[i] = newNode;
				i++;
			}
		}
		
		return result;
	}
	
	// Returns an array of possible nodes that can be arrived at from the passed node (unsorted).
	public TreeNode[] getMovesEvalOrdered(TreeNode node)
	{
		
		TreeNode[] result = new TreeNode[7];
		TreeNode newNode;
		GameStateModule newState;
		int i = 0;
		for (int x = 0; x < 7; x++)
		{
			if (terminate) return null;
			if (node.state.canMakeMove(x))
			{
				newState = node.state.copy();
				newState.makeMove(x);
				
				newNode = new TreeNode(newState, node);
				int evalValue = eval(newState);
				newNode.eval = evalValue;
				newNode.max = !node.max;
				newNode.lowestLevel = newNode.level = node.level + 1;
				newNode.move = x;
				
				insertNodeIntoArray(newNode, result);
			}
		}
		
		return result;
	}
	
	// Inserts the passed node into the array, dependent on the node being a min or max node.
	public void insertNodeIntoArray(TreeNode node, TreeNode[] ary)
	{
		TreeNode temp;
		for (int i = 0; i < 7; i++)
		{
			if (ary[i] == null)
			{
				ary[i] = node;
				break;
			}
			else
			{
				if (node.max)
				{ // Parent is a min-node (want increasing).
					if (node.eval < ary[i].eval)
					{
						temp = ary[i];
						ary[i] = node;
						node = temp;
					}
				}
				else
				{ // Parent is a max-node (want decreasing).
					if (node.eval > ary[i].eval)
					{
						temp = ary[i];
						ary[i] = node;
						node = temp;
					}
				}
			}
		}
	}
	
	// Returns the calculated evaluation value for the current board state.
	private int eval(final GameStateModule state)
	{
		if (state.isGameOver())
		{
			if (state.getWinner() == 0)
			{ // Draw game.
				return 0;
			}
			else if (state.getWinner() == ourPlayer)
			{ // We win.
				return Integer.MAX_VALUE - state.getCoins();
			}
			else
			{ // We lose.
				return Integer.MIN_VALUE + state.getCoins();
			}
		}
		
		boolean[] blockedMyWays = new boolean[69];
		boolean[] blockedOpponentWays = new boolean[69];
		int opponentPlayer = (ourPlayer == 1 ? 2 : 1);
		
		int myBlocked = 0;
		int opponentBlocked = 0;
		for (int x = 0; x < 7; x++)
		{
			for (int y = 0; y < state.getHeightAt(x); y++)
			{
				if (state.getAt(x, y) == ourPlayer)
				{
					opponentBlocked += waysToWinBlockXY(x, y, blockedOpponentWays);
				}
				else if (state.getAt(x, y) == opponentPlayer)
				{
					myBlocked += waysToWinBlockXY(x, y, blockedMyWays);
				}
			}
		}
		int result = opponentBlocked - myBlocked;
		
		return result;
	}
	
	// Returns the number of ways to win.
	private int waysToWinBlockXY(int x, int y, boolean[] blockedWays)
	{
		int pos = y * 7 + x;
		
		int result = 0;
		
		final int horizontalMulti = 1;
		final int verticalMulti = 1;
		final int diagonalMulti = 1;
		
		// Large switch statement to optimize way-blocking lookup.
		switch (pos)
		{
			case 0:
				result += horizontalMulti * waysToWinBlockWay(0, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(24, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(60, blockedWays);
				return result;
				
			case 1:
				result += horizontalMulti * waysToWinBlockWay(0, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(1, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(27, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(59, blockedWays);
				return result;
				
			case 2:
				result += horizontalMulti * waysToWinBlockWay(0, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(1, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(2, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(30, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(59, blockedWays);
				return result;
				
			case 3:
				result += horizontalMulti * waysToWinBlockWay(0, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(1, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(2, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(3, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(33, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(57, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(45, blockedWays);
				return result;
				
			case 4:
				result += horizontalMulti * waysToWinBlockWay(1, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(2, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(3, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(36, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(46, blockedWays);
				return result;
				
			case 5:
				result += horizontalMulti * waysToWinBlockWay(2, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(3, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(39, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(47, blockedWays);
				return result;
				
			case 6:
				result += horizontalMulti * waysToWinBlockWay(3, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(42, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(48, blockedWays);
				return result;
				
			case 7:
				result += horizontalMulti * waysToWinBlockWay(4, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(24, blockedWays);
				result += verticalMulti * waysToWinBlockWay(25, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(64, blockedWays);
				return result;
				
			case 8:
				result += horizontalMulti * waysToWinBlockWay(4, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(5, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(27, blockedWays);
				result += verticalMulti * waysToWinBlockWay(28, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(60, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(63, blockedWays);
				return result;
				
			case 9:
				result += horizontalMulti * waysToWinBlockWay(4, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(5, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(6, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(30, blockedWays);
				result += verticalMulti * waysToWinBlockWay(31, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(59, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(62, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(45, blockedWays);
				return result;
				
			case 10:
				result += horizontalMulti * waysToWinBlockWay(4, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(5, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(6, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(7, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(33, blockedWays);
				result += verticalMulti * waysToWinBlockWay(34, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(58, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(61, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(46, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(49, blockedWays);
				return result;
			
			case 11:
				result += horizontalMulti * waysToWinBlockWay(5, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(6, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(7, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(36, blockedWays);
				result += verticalMulti * waysToWinBlockWay(37, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(57, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(47, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(50, blockedWays);
				return result;
				
			case 12:
				result += horizontalMulti * waysToWinBlockWay(6, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(7, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(39, blockedWays);
				result += verticalMulti * waysToWinBlockWay(40, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(48, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(51, blockedWays);
				return result;
			
			case 13:
				result += horizontalMulti * waysToWinBlockWay(7, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(42, blockedWays);
				result += verticalMulti * waysToWinBlockWay(43, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(52, blockedWays);
				return result;
				
			case 14:
				result += horizontalMulti * waysToWinBlockWay(8, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(24, blockedWays);
				result += verticalMulti * waysToWinBlockWay(25, blockedWays);
				result += verticalMulti * waysToWinBlockWay(26, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(68, blockedWays);
				return result;
				
			case 15:
				result += horizontalMulti * waysToWinBlockWay(8, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(9, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(27, blockedWays);
				result += verticalMulti * waysToWinBlockWay(28, blockedWays);
				result += verticalMulti * waysToWinBlockWay(29, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(64, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(67, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(45, blockedWays);
				return result;
				
			case 16:
				result += horizontalMulti * waysToWinBlockWay(8, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(9, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(10, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(30, blockedWays);
				result += verticalMulti * waysToWinBlockWay(31, blockedWays);
				result += verticalMulti * waysToWinBlockWay(32, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(60, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(63, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(66, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(46, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(49, blockedWays);
				return result;
				
			case 17:
				result += horizontalMulti * waysToWinBlockWay(8, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(9, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(10, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(11, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(33, blockedWays);
				result += verticalMulti * waysToWinBlockWay(34, blockedWays);
				result += verticalMulti * waysToWinBlockWay(35, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(59, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(62, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(65, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(47, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(50, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(53, blockedWays);
				return result;
				
			case 18:
				result += horizontalMulti * waysToWinBlockWay(9, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(10, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(11, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(36, blockedWays);
				result += verticalMulti * waysToWinBlockWay(37, blockedWays);
				result += verticalMulti * waysToWinBlockWay(38, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(58, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(61, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(48, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(51, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(54, blockedWays);
				return result;
				
			case 19:
				result += horizontalMulti * waysToWinBlockWay(10, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(11, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(39, blockedWays);
				result += verticalMulti * waysToWinBlockWay(40, blockedWays);
				result += verticalMulti * waysToWinBlockWay(41, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(57, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(52, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(55, blockedWays);
				return result;
				
			case 20:
				result += horizontalMulti * waysToWinBlockWay(11, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(42, blockedWays);
				result += verticalMulti * waysToWinBlockWay(43, blockedWays);
				result += verticalMulti * waysToWinBlockWay(44, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(56, blockedWays);
				return result;
				
			case 21:
				result += horizontalMulti * waysToWinBlockWay(12, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(24, blockedWays);
				result += verticalMulti * waysToWinBlockWay(25, blockedWays);
				result += verticalMulti * waysToWinBlockWay(26, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(45, blockedWays);
				return result;
				
			case 22:
				result += horizontalMulti * waysToWinBlockWay(12, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(13, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(27, blockedWays);
				result += verticalMulti * waysToWinBlockWay(28, blockedWays);
				result += verticalMulti * waysToWinBlockWay(29, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(68, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(46, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(49, blockedWays);
				return result;
				
			case 23:
				result += horizontalMulti * waysToWinBlockWay(12, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(13, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(14, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(30, blockedWays);
				result += verticalMulti * waysToWinBlockWay(31, blockedWays);
				result += verticalMulti * waysToWinBlockWay(32, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(64, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(67, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(47, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(50, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(53, blockedWays);
				return result;
				
			case 24:
				result += horizontalMulti * waysToWinBlockWay(12, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(13, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(14, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(15, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(33, blockedWays);
				result += verticalMulti * waysToWinBlockWay(34, blockedWays);
				result += verticalMulti * waysToWinBlockWay(35, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(60, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(63, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(66, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(48, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(51, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(54, blockedWays);
				return result;
				
			case 25:
				result += horizontalMulti * waysToWinBlockWay(13, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(14, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(15, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(36, blockedWays);
				result += verticalMulti * waysToWinBlockWay(37, blockedWays);
				result += verticalMulti * waysToWinBlockWay(38, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(59, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(62, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(65, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(52, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(55, blockedWays);
				return result;
				
			case 26:
				result += horizontalMulti * waysToWinBlockWay(14, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(15, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(39, blockedWays);
				result += verticalMulti * waysToWinBlockWay(40, blockedWays);
				result += verticalMulti * waysToWinBlockWay(41, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(58, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(61, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(56, blockedWays);
				return result;
				
			case 27:
				result += horizontalMulti * waysToWinBlockWay(15, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(42, blockedWays);
				result += verticalMulti * waysToWinBlockWay(43, blockedWays);
				result += verticalMulti * waysToWinBlockWay(44, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(57, blockedWays);
				return result;
				
			case 28:
				result += horizontalMulti * waysToWinBlockWay(16, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(25, blockedWays);
				result += verticalMulti * waysToWinBlockWay(26, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(49, blockedWays);
				return result;
				
			case 29:
				result += horizontalMulti * waysToWinBlockWay(16, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(17, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(28, blockedWays);
				result += verticalMulti * waysToWinBlockWay(29, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(50, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(53, blockedWays);
				return result;
				
			case 30:
				result += horizontalMulti * waysToWinBlockWay(16, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(17, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(18, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(31, blockedWays);
				result += verticalMulti * waysToWinBlockWay(32, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(68, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(51, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(54, blockedWays);
				return result;
				
			case 31:
				result += horizontalMulti * waysToWinBlockWay(16, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(17, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(18, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(19, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(34, blockedWays);
				result += verticalMulti * waysToWinBlockWay(35, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(64, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(67, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(52, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(55, blockedWays);
				return result;
				
			case 32:
				result += horizontalMulti * waysToWinBlockWay(17, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(18, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(19, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(37, blockedWays);
				result += verticalMulti * waysToWinBlockWay(38, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(63, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(66, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(56, blockedWays);
				return result;
				
			case 33:
				result += horizontalMulti * waysToWinBlockWay(18, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(19, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(40, blockedWays);
				result += verticalMulti * waysToWinBlockWay(41, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(62, blockedWays);
				result += diagonalMulti * waysToWinBlockWay(65, blockedWays);
				return result;
				
			case 34:
				result += horizontalMulti * waysToWinBlockWay(19, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(43, blockedWays);
				result += verticalMulti * waysToWinBlockWay(44, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(61, blockedWays);
				return result;
				
			case 35:
				result += horizontalMulti * waysToWinBlockWay(20, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(26, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(53, blockedWays);
				return result;
				
			case 36:
				result += horizontalMulti * waysToWinBlockWay(20, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(21, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(29, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(54, blockedWays);
				return result;
				
			case 37:
				result += horizontalMulti * waysToWinBlockWay(20, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(21, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(22, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(32, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(55, blockedWays);
				return result;
				
			case 38:
				result += horizontalMulti * waysToWinBlockWay(20, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(21, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(22, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(23, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(35, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(68, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(56, blockedWays);
				return result;
				
			case 39:
				result += horizontalMulti * waysToWinBlockWay(21, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(22, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(23, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(38, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(67, blockedWays);
				return result;
				
			case 40:
				result += horizontalMulti * waysToWinBlockWay(22, blockedWays);
				result += horizontalMulti * waysToWinBlockWay(23, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(41, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(66, blockedWays);
				return result;
				
			case 41:
				result += horizontalMulti * waysToWinBlockWay(23, blockedWays);
				
				result += verticalMulti * waysToWinBlockWay(44, blockedWays);
				
				result += diagonalMulti * waysToWinBlockWay(65, blockedWays);
				return result;
		}
		return -1;
	}
	
	// Updates the blockedWays array.
	private int waysToWinBlockWay(int wayNum, boolean[] blockedWays)
	{
		if (blockedWays[wayNum] == false)
		{ // Block the way.
			blockedWays[wayNum] = true;
			if (wayNum == 4 || wayNum == 5 || wayNum == 6 || wayNum == 7 ||
				wayNum == 46 || wayNum == 49 || wayNum == 58 || wayNum == 61)
			{ // Passing through row 1.
				return 4;
			}
			else if (wayNum == 8 || wayNum == 9 || wayNum == 10 || wayNum == 11 ||
				wayNum == 47 || wayNum == 50 || wayNum == 53 ||
				wayNum == 59 || wayNum == 62 || wayNum == 65)
			{ // Passing through row 2.
				return 3;
			}
			else if (wayNum == 12 || wayNum == 13 || wayNum == 14 || wayNum == 15 ||
				wayNum == 48 || wayNum == 51 || wayNum == 54 ||
				wayNum == 60 || wayNum == 63 || wayNum == 66)
			{ // Passing through row 3.
				return 2;
			}
			else
			{
				return 1;
			}
		}
		else
		{ // The way was already blocked.
			return 0;
		}
	}
	
	// Displays the passed board state.
	private void printState(GameStateModule state)
	{
		IOModule io = new TextDisplay();
		io.drawBoard(state);
	}
	
	// Writes the passed tree to stdout.
	private void printTree(TreeNode node)
	{
		printState(node.state);
		System.out.println(node.level +  "   v: " + node.v + "   eval: " + node.eval);
		
		if (node.children != null)
		{
			for (int i = 0; i < 7; i++)
			{
				if (node.children[i] != null)
				{
					printTree(node.children[i]);
				}
			}
		}
	}
}

