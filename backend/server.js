require('dotenv').config();
const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const compression = require('compression');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');

const app = express();
app.use(compression());
app.use(cors());
app.use(express.json());

const JWT_SECRET = process.env.JWT_SECRET || 'ledgerly_secret_key_123456789';

// SCHEMAS & MODELS WITH INDEXES
const userSchema = new mongoose.Schema({
  name: { type: String, required: true, trim: true },
  email: { type: String, required: true, unique: true, lowercase: true, trim: true },
  passwordHash: { type: String, required: true },
  currency: { type: String, default: 'BDT' }
}, { timestamps: true });

const transactionSchema = new mongoose.Schema({
  userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true, index: true },
  type: { type: String, enum: ['income', 'expense'], required: true },
  amount: { type: Number, required: true, min: 0.01 },
  category: { type: String, required: true, trim: true },
  note: { type: String, default: '' },
  method: { type: String, enum: ['card', 'cash', 'bank', 'mobile', 'other'], default: 'cash' },
  date: { type: String, required: true } // yyyy-MM-dd
}, { timestamps: true });

transactionSchema.index({ userId: 1, date: -1 });

const salarySchema = new mongoose.Schema({
  userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true, index: true },
  source: { type: String, required: true, trim: true },
  amount: { type: Number, required: true, min: 0 },
  bonus: { type: Number, default: 0 },
  deduction: { type: Number, default: 0 },
  month: { type: String, required: true }, // yyyy-MM
  note: { type: String, default: '' }
}, { timestamps: true });

salarySchema.index({ userId: 1, month: -1 });

const budgetSchema = new mongoose.Schema({
  userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true, index: true },
  category: { type: String, required: true, trim: true }, // "Overall" or category name
  month: { type: String, required: true }, // yyyy-MM
  limit: { type: Number, required: true, min: 0 }
}, { timestamps: true });

budgetSchema.index({ userId: 1, category: 1, month: 1 }, { unique: true });

const User = mongoose.model('User', userSchema);
const Transaction = mongoose.model('Transaction', transactionSchema);
const Salary = mongoose.model('Salary', salarySchema);
const Budget = mongoose.model('Budget', budgetSchema);

// AUTH MIDDLEWARE
const authMiddleware = (req, res, next) => {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Unauthorized: Missing token' });
  }
  const token = authHeader.split(' ')[1];
  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    req.userId = new mongoose.Types.ObjectId(decoded.id);
    next();
  } catch (err) {
    return res.status(401).json({ error: 'Unauthorized: Invalid token' });
  }
};

// PUBLIC AUTH ROUTES
app.post('/api/auth/register', async (req, res) => {
  try {
    const { name, email, password } = req.body;
    if (!name || !email || !password) {
      return res.status(400).json({ error: 'Name, email and password are required' });
    }
    if (password.length < 6) {
      return res.status(400).json({ error: 'Password must be at least 6 characters' });
    }
    const existing = await User.findOne({ email: email.toLowerCase() }).lean();
    if (existing) {
      return res.status(409).json({ error: 'An account with this email already exists' });
    }
    const passwordHash = await bcrypt.hash(password, 10);
    const user = await User.create({ name, email, passwordHash, currency: 'BDT' });
    const token = jwt.sign({ id: user._id }, JWT_SECRET, { expiresIn: '7d' });
    res.status(201).json({
      token,
      user: { id: user._id, name: user.name, email: user.email, currency: user.currency }
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/auth/login', async (req, res) => {
  try {
    const { email, password } = req.body;
    if (!email || !password) {
      return res.status(400).json({ error: 'Email and password are required' });
    }
    const user = await User.findOne({ email: email.toLowerCase() }).lean();
    if (!user) {
      return res.status(401).json({ error: 'Invalid email or password' });
    }
    const match = await bcrypt.compare(password, user.passwordHash);
    if (!match) {
      return res.status(401).json({ error: 'Invalid email or password' });
    }
    const token = jwt.sign({ id: user._id }, JWT_SECRET, { expiresIn: '7d' });
    res.json({
      token,
      user: { id: user._id, name: user.name, email: user.email, currency: user.currency }
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// PROTECTED AUTH ROUTES
app.get('/api/auth/me', authMiddleware, async (req, res) => {
  try {
    const user = await User.findById(req.userId).lean();
    if (!user) return res.status(404).json({ error: 'User not found' });
    res.json({ id: user._id, name: user.name, email: user.email, currency: user.currency });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.put('/api/auth/profile', authMiddleware, async (req, res) => {
  try {
    const { name, currency } = req.body;
    const user = await User.findByIdAndUpdate(
      req.userId,
      { name, currency: currency || 'BDT' },
      { new: true, runValidators: true }
    ).lean();
    res.json({ id: user._id, name: user.name, email: user.email, currency: user.currency });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

app.put('/api/auth/password', authMiddleware, async (req, res) => {
  try {
    const { newPassword } = req.body;
    if (!newPassword || newPassword.length < 6) {
      return res.status(400).json({ error: 'Password must be at least 6 characters' });
    }
    const passwordHash = await bcrypt.hash(newPassword, 10);
    await User.findByIdAndUpdate(req.userId, { passwordHash });
    res.json({ message: 'Password updated successfully' });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// TRANSACTIONS ROUTES
app.get('/api/transactions', authMiddleware, async (req, res) => {
  try {
    const { type, category, search, from, to } = req.query;
    const query = { userId: req.userId };
    if (type && type !== 'All') query.type = type.toLowerCase();
    if (category && category !== 'All') query.category = new RegExp(`^${category}$`, 'i');
    if (search) {
      query.$or = [
        { note: new RegExp(search, 'i') },
        { category: new RegExp(search, 'i') }
      ];
    }
    if (from || to) {
      query.date = {};
      if (from) query.date.$gte = from;
      if (to) query.date.$lte = to;
    }
    const transactions = await Transaction.find(query).sort({ date: -1, _id: -1 }).lean();
    res.json(transactions);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/transactions', authMiddleware, async (req, res) => {
  try {
    const tx = await Transaction.create({ ...req.body, userId: req.userId });
    res.status(201).json(tx);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

app.put('/api/transactions/:id', authMiddleware, async (req, res) => {
  try {
    const tx = await Transaction.findOneAndUpdate(
      { _id: req.params.id, userId: req.userId },
      req.body,
      { new: true, runValidators: true }
    ).lean();
    if (!tx) return res.status(404).json({ error: 'Transaction not found' });
    res.json(tx);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

app.delete('/api/transactions/:id', authMiddleware, async (req, res) => {
  try {
    const tx = await Transaction.findOneAndDelete({ _id: req.params.id, userId: req.userId }).lean();
    if (!tx) return res.status(404).json({ error: 'Transaction not found' });
    res.json({ message: 'Deleted successfully' });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// SALARY ROUTES
app.get('/api/salary', authMiddleware, async (req, res) => {
  try {
    const salaries = await Salary.find({ userId: req.userId }).sort({ month: -1, _id: -1 }).lean();
    res.json(salaries);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/salary', authMiddleware, async (req, res) => {
  try {
    const sal = await Salary.create({ ...req.body, userId: req.userId });
    res.status(201).json(sal);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

app.put('/api/salary/:id', authMiddleware, async (req, res) => {
  try {
    const sal = await Salary.findOneAndUpdate(
      { _id: req.params.id, userId: req.userId },
      req.body,
      { new: true, runValidators: true }
    ).lean();
    if (!sal) return res.status(404).json({ error: 'Salary record not found' });
    res.json(sal);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

app.delete('/api/salary/:id', authMiddleware, async (req, res) => {
  try {
    const sal = await Salary.findOneAndDelete({ _id: req.params.id, userId: req.userId }).lean();
    if (!sal) return res.status(404).json({ error: 'Salary record not found' });
    res.json({ message: 'Deleted successfully' });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// BUDGET ROUTES
app.get('/api/budgets', authMiddleware, async (req, res) => {
  try {
    const { month } = req.query;
    const currentMonth = month || new Date().toISOString().substring(0, 7);

    const [budgets, expenses] = await Promise.all([
      Budget.find({ userId: req.userId, month: currentMonth }).lean(),
      Transaction.find({
        userId: req.userId,
        type: 'expense',
        date: new RegExp(`^${currentMonth}`)
      }).lean()
    ]);

    const result = budgets.map(b => {
      let spent = 0;
      if (b.category.toLowerCase() === 'overall') {
        spent = expenses.reduce((sum, e) => sum + e.amount, 0);
      } else {
        spent = expenses
          .filter(e => e.category.toLowerCase() === b.category.toLowerCase())
          .reduce((sum, e) => sum + e.amount, 0);
      }
      return {
        _id: b._id,
        category: b.category,
        month: b.month,
        limit: b.limit,
        spent
      };
    });

    res.json(result);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/budgets', authMiddleware, async (req, res) => {
  try {
    const { category, month, limit } = req.body;
    const b = await Budget.findOneAndUpdate(
      { userId: req.userId, category, month },
      { limit },
      { upsert: true, new: true, runValidators: true }
    ).lean();
    res.status(201).json(b);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

app.delete('/api/budgets/:id', authMiddleware, async (req, res) => {
  try {
    const b = await Budget.findOneAndDelete({ _id: req.params.id, userId: req.userId }).lean();
    if (!b) return res.status(404).json({ error: 'Budget not found' });
    res.json({ message: 'Deleted successfully' });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// DASHBOARD ROUTE (FAST PROMISE.ALL AGGREGATION)
app.get('/api/dashboard', authMiddleware, async (req, res) => {
  try {
    const now = new Date();
    const currentMonth = now.toISOString().substring(0, 7); // yyyy-MM

    const [allTxs, currentSalaries, recent] = await Promise.all([
      Transaction.find({ userId: req.userId }).lean(),
      Salary.find({ userId: req.userId, month: currentMonth }).lean(),
      Transaction.find({ userId: req.userId }).sort({ date: -1, _id: -1 }).limit(5).lean()
    ]);

    let balance = 0;
    let monthIncome = 0;
    let monthExpense = 0;

    allTxs.forEach(tx => {
      if (tx.type === 'income') balance += tx.amount;
      else if (tx.type === 'expense') balance -= tx.amount;

      if (tx.date.startsWith(currentMonth)) {
        if (tx.type === 'income') monthIncome += tx.amount;
        else if (tx.type === 'expense') monthExpense += tx.amount;
      }
    });

    const remaining = monthIncome - monthExpense;
    const monthlySalary = currentSalaries.reduce((sum, s) => sum + (s.amount + (s.bonus || 0) - (s.deduction || 0)), 0);

    // Last 6 months Income vs Expense
    const last6Months = [];
    const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    for (let i = 5; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
      const mStr = d.toISOString().substring(0, 7);
      const label = monthNames[d.getMonth()];

      let inc = 0;
      let exp = 0;
      allTxs.forEach(tx => {
        if (tx.date.startsWith(mStr)) {
          if (tx.type === 'income') inc += tx.amount;
          else if (tx.type === 'expense') exp += tx.amount;
        }
      });
      last6Months.push({ label, income: inc, expense: exp });
    }

    // Category breakdown for current month's expenses
    const catMap = {};
    allTxs.filter(tx => tx.type === 'expense' && tx.date.startsWith(currentMonth)).forEach(tx => {
      catMap[tx.category] = (catMap[tx.category] || 0) + tx.amount;
    });
    const categoryBreakdown = Object.keys(catMap).map(cat => ({ category: cat, total: catMap[cat] }));

    res.json({
      balance,
      monthIncome,
      monthExpense,
      remaining,
      monthlySalary,
      last6Months,
      categoryBreakdown,
      recent
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// REPORTS ROUTE
app.get('/api/reports', authMiddleware, async (req, res) => {
  try {
    const { range } = req.query; // 7d, 4w, 6m, year
    const allTxs = await Transaction.find({ userId: req.userId }).lean();

    const now = new Date();
    let startDate = new Date();

    if (range === '7d') startDate.setDate(now.getDate() - 7);
    else if (range === '4w') startDate.setDate(now.getDate() - 28);
    else if (range === '6m') startDate.setMonth(now.getMonth() - 6);
    else if (range === 'year') startDate.setFullYear(now.getFullYear() - 1);
    else startDate.setDate(now.getDate() - 30);

    const startStr = startDate.toISOString().substring(0, 10);
    const filteredTxs = allTxs.filter(tx => tx.date >= startStr);

    const seriesMap = {};
    filteredTxs.forEach(tx => {
      const key = tx.date;
      if (!seriesMap[key]) seriesMap[key] = { income: 0, expense: 0 };
      if (tx.type === 'income') seriesMap[key].income += tx.amount;
      else if (tx.type === 'expense') seriesMap[key].expense += tx.amount;
    });

    const series = Object.keys(seriesMap).sort().map(d => ({
      label: d.substring(5), // MM-dd
      income: seriesMap[d].income,
      expense: seriesMap[d].expense
    }));

    const catMap = {};
    filteredTxs.filter(tx => tx.type === 'expense').forEach(tx => {
      catMap[tx.category] = (catMap[tx.category] || 0) + tx.amount;
    });
    const byCategory = Object.keys(catMap).map(cat => ({ category: cat, total: catMap[cat] }));

    res.json({ series, byCategory });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// START SERVER WITH CONNECT OPTIONS
const PORT = process.env.PORT || 3000;
mongoose.connect(process.env.MONGO_URI, {
  maxPoolSize: 10,
  serverSelectionTimeoutMS: 5000,
  socketTimeoutMS: 45000
}).then(() => {
  console.log('MongoDB connected');
  app.listen(PORT, '0.0.0.0', () => console.log('Ledgerly Server running on port ' + PORT));
}).catch((e) => console.error('MongoDB connection error:', e.message));
