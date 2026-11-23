import "./globals.css";

export const metadata = {
  title: "Orders | The essentials shop",
  description: "A small shop powered by the orders demo.",
};

export default function RootLayout({ children }) {
  return <html lang="en"><body>{children}</body></html>;
}
