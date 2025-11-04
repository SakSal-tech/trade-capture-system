import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { observer } from "mobx-react-lite";
import SignIn from "./pages/SignIn";
import Main from "./pages/Main";
import userStore from "./stores/userStore";
// TraderSales page removed from top-level routes; instructions were
// consolidated in the TradeActionsModal. Import kept out to avoid
// accidental usage.
import MiddleOffice from "./pages/MiddleOffice";
import Support from "./pages/Support";
import Admin from "./pages/Admin";
import TradeDashboard from "./pages/TradeDashboard";
import TraderSales from "./pages/TraderSales";
import TradeBooking from "./pages/TradeBooking";
import PrivateRoute from "./PrivateRoute";

const AppRouter = observer(() => (
  <BrowserRouter>
    <Routes>
      <Route path="/signin" element={<SignIn />} />
      <Route
        path="/home"
        element={
          <PrivateRoute>
            <Main />
          </PrivateRoute>
        }
      />
      {/* /trade routes removed per request; navigation updated to use Home/Dashboard */}
      <Route
        path="/trade"
        element={
          <PrivateRoute>
            <TraderSales />
          </PrivateRoute>
        }
      />
      <Route
        path="/trade/book"
        element={
          <PrivateRoute>
            <TradeBooking />
          </PrivateRoute>
        }
      />
      <Route
        path="/dashboard"
        element={
          <PrivateRoute>
            <TradeDashboard />
          </PrivateRoute>
        }
      />
      <Route
        path="/middle-office"
        element={
          <PrivateRoute>
            <MiddleOffice />
          </PrivateRoute>
        }
      />
      <Route
        path="/support"
        element={
          <PrivateRoute>
            <Support />
          </PrivateRoute>
        }
      />
      <Route
        path="/admin"
        element={
          <PrivateRoute>
            <Admin />
          </PrivateRoute>
        }
      />
      <Route
        path="*"
        element={
          userStore.user ? (
            <Navigate to="/home" replace />
          ) : (
            <Navigate to="/signin" replace />
          )
        }
      />
    </Routes>
  </BrowserRouter>
));

export default AppRouter;
