const systemInfoFixtures = {
  showingBoth: {
    springH2ConsoleEnabled: true,
    showSwaggerUILink: true,
    oauthLogin: "/oauth2/authorization/google",
  },
  showingNeither: {
    springH2ConsoleEnabled: false,
    showSwaggerUILink: false,
    oauthLogin: "/oauth2/authorization/google",
  },
  oauthLoginUndefined: {
    springH2ConsoleEnabled: false,
    showSwaggerUILink: false,
  },
  showingWithRepoInfo: {
    springH2ConsoleEnabled: true,
    showSwaggerUILink: true,
    oauthLogin: "/oauth2/authorization/google",
    sourceRepo: "https://github.com/ucsb-cs156-s26/proj-dining-s26-05",
    githubUrl:
      "https://github.com/ucsb-cs156-s26/proj-dining-s26-05/commit/abc1234",
    commitId: "abc1234",
    commitMessage: "Test commit message",
  },
};

export { systemInfoFixtures };
