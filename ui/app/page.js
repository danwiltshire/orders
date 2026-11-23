import { getCatalog } from "../lib/backend.js";
import OrderConsole from "./order-console.js";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

export default async function Home() {
  let products;
  try {
    products = await getCatalog();
  } catch (error) {
    console.error("Could not load catalog:", error);
  }

  return (
    <>
      <header className="header">
        <a className="brand" href="/" aria-label="Orders home"><span className="brand-mark">o</span> orders<span className="brand-dot">.</span></a>
        <span className="header-label">A little shop for your desk</span>
      </header>
      <main>
        <section className="hero">
          <div>
            <p className="eyebrow"><span className="eyebrow-dot" /> THE EVERYDAY EDIT</p>
            <h1>Good things,<br /><em>in order.</em></h1>
            <p className="intro">Thoughtful essentials for the places where ideas happen. Pick something you like and watch your order come to life.</p>
            <a className="hero-link" href="#collection">Explore the collection <span aria-hidden="true">↓</span></a>
          </div>
          <div className="hero-art" aria-hidden="true">
            <div className="art-circle"><span className="art-spark">✳</span><span className="art-label">A little<br />more joy<br />at your desk.</span></div>
            <span className="art-caption">OBJECTS FOR EVERY DAY / 001</span>
          </div>
        </section>
        <section className="shop" id="collection">
          <div className="section-heading">
            <div><p className="eyebrow">THE COLLECTION</p><h2>Small things, big ideas<span className="brand-dot">.</span></h2></div>
            <p>Made for everyday moments.</p>
          </div>
          {products ? <OrderConsole products={products} /> : <p className="notice" role="alert">The catalog is temporarily unavailable. Please try again later.</p>}
        </section>
      </main>
      <footer><span className="footer-brand">orders<span className="brand-dot">.</span></span><span>A little demo with a lot going on behind the scenes.</span></footer>
    </>
  );
}
