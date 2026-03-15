/** @type {import('tailwindcss').Config} */
module.exports = {
  mode: process.env.NODE_ENV ? 'jit' : undefined,
  darkMode: "media",
  content: ["./src/**/*.jte", "./src/**/*.js"],
  theme: {
    extend: {},
  },
  plugins: [],
}

