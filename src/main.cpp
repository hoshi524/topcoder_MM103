#include <bits/stdc++.h>
using namespace std;

constexpr int W = 1800;
constexpr int I = 101;
constexpr int P = 101;
constexpr double Q = 0.05;
double DP[W][P];
double BE[I][I][I];
double DE[I][I][I];
double CE[I];
int n;
int day;
int score;
vector<int> buy;
vector<int> sell;
vector<int> expires;
int S[I];
int B[I][I];

inline double endday(int d) {
  if (d < 10) return 0.0;
  if (d > 99) return 0.0;
  return 1.0 / (100.0 - max(day, 10));
}

inline void updateCE(int x) {
  double sum = 0;
  for (int c = 0; c < I; ++c) {
    sum += (CE[c] *= DP[c][x]);
  }
  for (int c = 0; c < I; ++c) {
    CE[c] /= sum;
  }
}

inline int V(int i) {
  int b = buy[i];
  int e = expires[i];
  int v = 0;
  double w = -1e10;
  double rd = ((100 + max(day, 10)) / 2.0 - day);
  for (int k = 0; k < 15; ++k) {
    double x = 0;
    for (int y = 0; y < I; ++y) {
      if (CE[y] < 1e-4) continue;
      double z = BE[i][k][y] - DE[i][k][y] * rd;
      for (int j = 0; j < k; ++j) {
        double a = 1;
        double u = b * (k - j);
        for (int d = 1; d <= e; ++d) {
          double f = endday(day + d - 1);
          z -= (d == e ? a : f) * u * DP[y * d][j] / d;
          a -= f;
        }
      }
      x += z * CE[y];
    }
    if (w < x) {
      w = x;
      v = k;
    }
  }
  return v;
}

class ProductInventory {
 public:
  int init(vector<int> &buy_, vector<int> &sell_, vector<int> &expires_) {
    day = 0;
    score = 0;
    buy = buy_;
    sell = sell_;
    expires = expires_;
    n = buy.size();
    for (int i = 0; i < n; ++i) {
      expires[i]++;
    }
    double T[I];
    memset(T, 0, sizeof(T));
    memset(S, 0, sizeof(S));
    memset(B, 0, sizeof(B));
    memset(DP, 0, sizeof(DP));
    memset(CE, 0, sizeof(CE));
    memset(BE, 0, sizeof(BE));
    memset(DE, 0, sizeof(DE));
    DP[0][0] = 1;
    for (int i = 0; i + 1 < W; ++i) {
      for (int j = 0; j < P; ++j) {
        if (j + 1 < P) {
          DP[i + 1][j + 1] += DP[i][j] * Q;
          DP[i + 1][j + 0] += DP[i][j] * (1 - Q);
        } else {
          DP[i + 1][j + 0] += DP[i][j];
        }
      }
    }
    for (int p = 1; p < I; ++p) {
      for (int i = 0; i < n; ++i) {
        int b = buy[i];
        int s = sell[i];
        int e = expires[i];
        int z = p * e;
        double t = -b * DP[z][0];
        for (int j = 1; j < P; ++j) {
          t += (s - b) * j * DP[z][j];
        }
        T[p] += t / z;
      }
    }
    for (int c = 50; c < I; ++c) {
      CE[c] = 1;
    }
    for (int i = 0; i < n; ++i) {
      for (int j = 0; j < I; ++j) {
        for (int k = 0; k < I; ++k) {
          for (int p = 0; p <= k; ++p) {
            BE[i][j][k] += DP[k][p] * (sell[i] - buy[i]) * min(j, p);
            DE[i][j][k] += DP[k][p] * T[k] * Q * max(p - j, 0);
          }
        }
      }
    }
    return 0;
  }

  vector<int> order(vector<int> &yesterday) {
    if (day) {
      int s = 0;
      int c = 0;
      for (int i = 0; i < n; ++i) {
        if (S[i] > 10) {
          s += yesterday[i];
          c++;
        }
        for (int d = day, y = yesterday[i]; d < I && y > 0; ++d) {
          if (B[i][d]) {
            int t = min(B[i][d], y);
            B[i][d] -= t;
            y -= t;
          }
        }
        S[i] -= yesterday[i] + B[i][day];
        score += sell[i] * yesterday[i];
      }
      if (c) updateCE(s / c);
    } else {
      for (int i = 0; i < n; ++i) {
        updateCE(yesterday[i] / 10);
      }
    }
    vector<int> order(n, 0);
    for (int i = 0; i < n; ++i) {
      int a = V(i) - S[i];
      if (a > 0) {
        order[i] += a;
        S[i] += a;
        score -= buy[i] * a;
        int d = day + expires[i];
        if (d < I) B[i][d] += a;
      }
    }
    day++;
    return order;
  }
};

int main() {
  int n;
  vector<int> buy;
  vector<int> sell;
  vector<int> expiration;
  vector<int> yesterday;
  vector<int> res;

  cin >> n;
  buy.resize(n);
  for (int i = 0; i < n; i++) cin >> buy[i];
  cin >> n;
  sell.resize(n);
  for (int i = 0; i < n; i++) cin >> sell[i];
  cin >> n;
  expiration.resize(n);
  for (int i = 0; i < n; i++) cin >> expiration[i];

  ProductInventory sol;
  cout << sol.init(buy, sell, expiration) << endl;
  cout << flush;

  for (int k = 0; k < 100; k++) {
    cin >> n;
    yesterday.resize(n);
    for (int i = 0; i < n; i++) {
      cin >> yesterday[i];
    }

    res = sol.order(yesterday);

    cout << res.size() << endl;
    for (int i = 0; i < (int)res.size(); i++) {
      cout << res[i] << endl;
    }
    cout << flush;
  }
}